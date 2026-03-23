/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.sqlobject.customizer.internal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.ArgumentTestAccessor;
import org.jdbi.v3.core.argument.ObjectArgument;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.MockClock;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.customizer.Timestamped;
import org.jdbi.v3.sqlobject.customizer.TimestampedConfig;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link Timestamped} annotation
 */
public class TestTimestamped {

    private static final ZoneOffset GMT_PLUS_2 = ZoneOffset.ofHours(2);
    private static final OffsetDateTime UTC_MOMENT = OffsetDateTime.of(LocalDate.of(2018, Month.JANUARY, 1), LocalTime.NOON, ZoneOffset.UTC);

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin());

    private static ThreadLocal<String> logNext = new ThreadLocal<>();
    private static ThreadLocal<OffsetDateTime> insertedTimestamp = new ThreadLocal<>();

    private final MockClock clock = MockClock.at(UTC_MOMENT.toZonedDateTime());

    private Jdbi db;

    @BeforeEach
    public void before() {
        TimestampedFactory.setTimeSource(clock::withZone);
        db = h2Extension.getJdbi();
        db.getConfig(TimestampedConfig.class).setTimezone(GMT_PLUS_2);

        db.setSqlLogger(new SqlLogger() {
            @Override
            public void logBeforeExecution(StatementContext ctx) {
                String name = logNext.get();
                if (name != null) {
                    var argument = ctx.getBinding()
                        .findForName(name, ctx);

                    argument.ifPresent(arg -> {
                        OffsetDateTime value = arg instanceof ObjectArgument objectArgument
                            ? ArgumentTestAccessor.getObjectArgumentValue(OffsetDateTime.class, objectArgument)
                            : OffsetDateTime.parse(arg.toString());
                        insertedTimestamp.set(value);
                        logNext.remove();
                    });
                }
            }
        });
    }

    @AfterEach
    void after() {
        db.useExtension(CreateDAO.class, CreateDAO::dropTable);
    }

    static Stream<Consumer<CreateDAO>> daoProvider() {
        return Stream.of(
            new Consumer<>() {
                @Override
                public void accept(CreateDAO dao) {
                    dao.createTable();
                }

                @Override
                public String toString() {
                    return "no timezone";
                }
            },
            new Consumer<>() {
                @Override
                public void accept(CreateDAO dao) {
                    dao.createTSTZTable();
                }

                @Override
                public String toString() {
                    return "with timezone";
                }
            });
    }

    static Stream<TestMapper<?>> mapperProvider() {
        return Stream.of(new TimestampPersonRowMapper(), new OffsetDateTimePersonRowMapper(), new ZonedDateTimePersonRowMapper(), new InstantPersonRowMapper());
    }

    static Stream<Arguments> argumentsProvider() {
        List<Arguments> arguments = new ArrayList<>();
        daoProvider().forEach(dao -> mapperProvider().forEach(mapper -> arguments.add(Arguments.of(dao, mapper))));
        return arguments.stream();
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    public void shouldInsertCreatedAndModifiedFields(Consumer<CreateDAO> consumer, TestMapper<?> mapper) {
        db.useExtension(CreateDAO.class, consumer::accept);

        db.registerRowMapper(Person.class, mapper);
        db.useExtension(PersonDAO.class, dao -> {

            Person<?> input = new TimestampPerson(1, "John", "Phiri");

            logNext.set("now");
            dao.insert(input);
            assertThat(insertedTimestamp.get().getOffset()).isEqualTo(GMT_PLUS_2);
            assertThat(insertedTimestamp.get().toInstant()).isEqualTo(UTC_MOMENT.toInstant());

            Person<?> result = dao.get(1);

            assertThat(mapper.toInstant(result.created()))
                .isEqualTo(mapper.toInstant(result.modified()))
                .isEqualTo(insertedSqlTimestamp());
        });
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    public void shouldAllowCustomTimestampParameter(Consumer<CreateDAO> consumer, TestMapper<?> mapper) {
        db.useExtension(CreateDAO.class, consumer::accept);
        db.registerRowMapper(Person.class, mapper);

        db.useExtension(PersonDAO.class, dao -> {
            Person<?> input = new TimestampPerson(1, "John", "Phiri");

            logNext.set("createdAt");
            dao.insertWithCustomTimestampFields(input);
            assertThat(insertedTimestamp.get().getOffset()).isEqualTo(GMT_PLUS_2);
            assertThat(insertedTimestamp.get().toInstant()).isEqualTo(UTC_MOMENT.toInstant());

            Person<?> result = dao.get(1);

            assertThat(result.firstName()).isEqualTo(input.firstName());
            assertThat(result.lastName()).isEqualTo(input.lastName());
            assertThat(mapper.toInstant(result.created()))
                .isEqualTo(mapper.toInstant(result.modified()))
                .isEqualTo(insertedSqlTimestamp());
        });
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    public void shouldUpdateModifiedTimestamp(Consumer<CreateDAO> consumer, TestMapper<?> mapper) {
        db.useExtension(CreateDAO.class, consumer::accept);
        db.registerRowMapper(Person.class, mapper);

        db.useExtension(PersonDAO.class, dao -> {
            Person<?> input = new TimestampPerson(3, "John", "Phiri");

            logNext.set("now");
            dao.insert(input);
            Instant insert = insertedSqlTimestamp();

            Person<?> fetched = dao.get(3);
            fetched = fetched.withLastName("Banda");
            clock.advance(1, ChronoUnit.SECONDS);

            logNext.set("now");
            dao.updatePerson(fetched);
            Instant update = insertedSqlTimestamp();

            Person<?> result = dao.get(3);

            assertThat(insert).isNotEqualTo(update);
            assertThat(result.lastName()).isEqualToIgnoringCase("Banda");
            assertThat(mapper.toInstant(result.created())).isEqualTo(insert);
            assertThat(mapper.toInstant(result.modified())).isEqualTo(update);
        });
    }

    private static Instant insertedSqlTimestamp() {
        return insertedTimestamp.get().toInstant();
    }

    public interface CreateDAO {
        @SqlUpdate("CREATE TABLE people(id identity primary key, firstName varchar(50), lastName varchar(50), created timestamp, modified timestamp);")
        void createTable();

        @SqlUpdate("CREATE TABLE people(id identity primary key, firstName varchar(50), lastName varchar(50), created timestamp with time zone, modified timestamp with time zone);")
        void createTSTZTable();

        @SqlUpdate("DROP TABLE IF EXISTS people")
        void dropTable();
    }

    public interface PersonDAO {
        @GetGeneratedKeys
        @SqlUpdate("INSERT INTO people(id, firstName, lastName, created, modified) VALUES (:p.id, :p.firstName, :p.lastName, :now, :now)")
        @Timestamped
        int insert(@BindMethods("p") Person<?> person);

        @SqlUpdate("INSERT INTO people(id, firstName, lastName, created, modified) VALUES (:p.id, :p.firstName, :p.lastName, :createdAt, :createdAt)")
        @Timestamped("createdAt")
        void insertWithCustomTimestampFields(@BindMethods("p") Person<?> person);

        @SqlUpdate("UPDATE people SET firstName = :p.firstName, lastName = :p.lastName, modified = :now WHERE id = :p.id")
        @Timestamped
        void updatePerson(@BindMethods("p") Person<?> person);

        @SqlQuery("SELECT id, firstName, lastName, created, modified from people WHERE id=:id")
        Person get(@Bind("id") int id);
    }

    public interface Person<T> {
        int id();

        String firstName();

        String lastName();

        T created();

        T modified();

        Person<T> withLastName(String lastName);
    }

    public interface TestMapper<T> extends RowMapper<Person<T>> {
        Instant toInstant(Object type);
    }

    public static final class TimestampPersonRowMapper implements TestMapper<Timestamp> {
        @Override
        public TimestampPerson map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
            return new TimestampPerson(resultSet.getInt("id"),
                resultSet.getString("firstName"),
                resultSet.getString("lastName"),
                resultSet.getTimestamp("created"),
                resultSet.getTimestamp("modified"));
        }

        @Override
        public String toString() {
            return "mapper with timestamps";
        }

        @Override
        public Instant toInstant(Object type) {
            return ((Timestamp) type).toInstant();
        }
    }

    public record TimestampPerson(int id, String firstName, String lastName, Timestamp created, Timestamp modified) implements Person<Timestamp> {
        public TimestampPerson(int id, String firstName, String lastName) {
            this(id, firstName, lastName, null, null);
        }

        public TimestampPerson withLastName(String lastName) {
            return new TimestampPerson(this.id, this.firstName, lastName, this.created, this.modified);
        }
    }

    public static final class OffsetDateTimePersonRowMapper implements TestMapper<OffsetDateTime> {
        @Override
        public OffsetDateTimePerson map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
            return new OffsetDateTimePerson(resultSet.getInt("id"),
                resultSet.getString("firstName"),
                resultSet.getString("lastName"),
                resultSet.getObject("created", OffsetDateTime.class),
                resultSet.getObject("modified", OffsetDateTime.class));
        }

        @Override
        public String toString() {
            return "mapper with offset date time";
        }

        @Override
        public Instant toInstant(Object type) {
            return ((OffsetDateTime) type).toInstant();
        }
    }

    public record OffsetDateTimePerson(int id, String firstName, String lastName, OffsetDateTime created, OffsetDateTime modified) implements
        Person<OffsetDateTime> {
        public OffsetDateTimePerson(int id, String firstName, String lastName) {
            this(id, firstName, lastName, null, null);
        }

        public OffsetDateTimePerson withLastName(String lastName) {
            return new OffsetDateTimePerson(this.id, this.firstName, lastName, this.created, this.modified);
        }
    }

    public static final class ZonedDateTimePersonRowMapper implements TestMapper<ZonedDateTime> {
        @Override
        public ZonedDateTimePerson map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
            return new ZonedDateTimePerson(resultSet.getInt("id"),
                resultSet.getString("firstName"),
                resultSet.getString("lastName"),
                resultSet.getObject("created", ZonedDateTime.class),
                resultSet.getObject("modified", ZonedDateTime.class));
        }

        @Override
        public Instant toInstant(Object type) {
            return ((ZonedDateTime) type).toInstant();
        }

        @Override
        public String toString() {
            return "mapper with zoned date time";
        }
    }

    public record ZonedDateTimePerson(int id, String firstName, String lastName, ZonedDateTime created, ZonedDateTime modified) implements
        Person<ZonedDateTime> {
        public ZonedDateTimePerson(int id, String firstName, String lastName) {
            this(id, firstName, lastName, null, null);
        }

        public ZonedDateTimePerson withLastName(String lastName) {
            return new ZonedDateTimePerson(this.id, this.firstName, lastName, this.created, this.modified);
        }
    }

    public static final class InstantPersonRowMapper implements TestMapper<Instant> {
        @Override
        public InstantPerson map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
            return new InstantPerson(resultSet.getInt("id"),
                resultSet.getString("firstName"),
                resultSet.getString("lastName"),
                resultSet.getObject("created", Instant.class),
                resultSet.getObject("modified", Instant.class));
        }

        @Override
        public Instant toInstant(Object type) {
            return ((Instant) type);
        }

        @Override
        public String toString() {
            return "mapper with instant";
        }
    }

    public record InstantPerson(int id, String firstName, String lastName, Instant created, Instant modified) implements
        Person<Instant> {
        public InstantPerson(int id, String firstName, String lastName) {
            this(id, firstName, lastName, null, null);
        }

        public InstantPerson withLastName(String lastName) {
            return new InstantPerson(this.id, this.firstName, lastName, this.created, this.modified);
        }
    }
}
