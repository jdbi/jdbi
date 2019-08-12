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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.MockClock;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Timestamped;
import org.jdbi.v3.sqlobject.customizer.TimestampedConfig;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link Timestamped} annotation
 */
public class TestTimestamped {
    private static final ZoneOffset GMT_PLUS_2 = ZoneOffset.ofHours(2);
    private static final OffsetDateTime UTC_MOMENT = OffsetDateTime.of(LocalDate.of(2018, Month.JANUARY, 1), LocalTime.NOON, ZoneOffset.UTC);

    @Rule
    public JdbiRule dbRule = JdbiRule.h2().withPlugin(new SqlObjectPlugin());
    private PersonDAO personDAO;

    private static ThreadLocal<String> logNext = new ThreadLocal<>();
    private static ThreadLocal<OffsetDateTime> insertedTimestamp = new ThreadLocal<>();

    private final MockClock clock = MockClock.at(UTC_MOMENT.toZonedDateTime());

    @Before
    public void before() {
        TimestampedFactory.setTimeSource(clock::withZone);
        final Jdbi db = dbRule.getJdbi();
        db.getConfig(TimestampedConfig.class).setTimezone(GMT_PLUS_2);

        db.setSqlLogger(new SqlLogger() {
            @Override
            public void logBeforeExecution(StatementContext ctx) {
                String name = logNext.get();
                if (name != null) {
                    String toString = ctx.getBinding()
                        .findForName(name, ctx)
                        .orElseThrow(AssertionError::new)
                        .toString();
                    insertedTimestamp.set(OffsetDateTime.parse(toString));
                    logNext.set(null);
                }
            }
        });

        personDAO = db.onDemand(PersonDAO.class);
        personDAO.createTable();
    }

    @Test
    public void shouldInsertCreatedAndModifiedFields() {
        Person input = new Person("John", "Phiri");
        input.setId(1);

        logNext.set("now");
        personDAO.insert(input);
        assertThat(insertedTimestamp.get().getOffset()).isEqualTo(GMT_PLUS_2);
        assertThat(insertedTimestamp.get().toInstant()).isEqualTo(UTC_MOMENT.toInstant());

        Person result = personDAO.get(1);

        assertThat(result.getCreated())
            .isEqualTo(result.getModified())
            .isEqualTo(insertedSqlTimestamp());
    }

    @Test
    public void shouldAllowCustomTimestampParameter() {
        Person input = new Person("John", "Phiri");
        input.setId(1);

        logNext.set("createdAt");
        personDAO.insertWithCustomTimestampFields(input);
        assertThat(insertedTimestamp.get().getOffset()).isEqualTo(GMT_PLUS_2);
        assertThat(insertedTimestamp.get().toInstant()).isEqualTo(UTC_MOMENT.toInstant());

        Person result = personDAO.get(1);

        assertThat(result.getFirstName()).isEqualTo(input.getFirstName());
        assertThat(result.getLastName()).isEqualTo(input.getLastName());
        assertThat(result.getCreated())
            .isEqualTo(result.getModified())
            .isEqualTo(insertedSqlTimestamp());
    }

    @Test
    public void shouldUpdateModifiedTimestamp() {
        Person input = new Person("John", "Phiri");
        input.setId(3);

        logNext.set("now");
        personDAO.insert(input);
        Timestamp insert = insertedSqlTimestamp();

        Person fetched = personDAO.get(3);
        fetched.setLastName("Banda");
        clock.advance(1, ChronoUnit.SECONDS);

        logNext.set("now");
        personDAO.updatePerson(fetched);
        Timestamp update = insertedSqlTimestamp();

        Person result = personDAO.get(3);

        assertThat(insert).isNotEqualTo(update);
        assertThat(result.getLastName()).isEqualToIgnoringCase("Banda");
        assertThat(result.getCreated()).isEqualTo(insert);
        assertThat(result.getModified()).isEqualTo(update);
    }

    private static Timestamp insertedSqlTimestamp() {
        return Timestamp.from(insertedTimestamp.get().toInstant());
    }

    @RegisterRowMapper(PersonRowMapper.class)
    public interface PersonDAO {
        @SqlUpdate("CREATE TABLE people(id identity primary key, firstName varchar(50), lastName varchar(50), created timestamp, modified timestamp);")
        void createTable();

        @GetGeneratedKeys
        @SqlUpdate("INSERT INTO people(id, firstName, lastName, created, modified) VALUES (:p.id, :p.firstName, :p.lastName, :now, :now)")
        @Timestamped
        int insert(@BindBean("p") Person person);

        @SqlUpdate("INSERT INTO people(id, firstName, lastName, created, modified) VALUES (:p.id, :p.firstName, :p.lastName, :createdAt, :createdAt)")
        @Timestamped("createdAt")
        int insertWithCustomTimestampFields(@BindBean("p") Person person);

        @SqlUpdate("UPDATE people SET firstName = :p.firstName, lastName = :p.lastName, modified = :now WHERE id = :p.id")
        @Timestamped
        int updatePerson(@BindBean("p") Person person);

        @SqlQuery("SELECT id, firstName, lastName, created, modified from people WHERE id=:id")
        Person get(@Bind("id") int id);
    }

    public static final class PersonRowMapper implements RowMapper<Person> {
        @Override
        public Person map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
            Person person = new Person(resultSet.getString("firstName"), resultSet.getString("lastName"));
            person.setId(resultSet.getInt("id"));
            person.setCreated(resultSet.getTimestamp("created"));
            person.setModified(resultSet.getTimestamp("modified"));
            return person;
        }
    }

    /**
     * Person JavaBean for tests
     */
    public static final class Person {
        private int id;

        private String firstName;

        private String lastName;

        private Timestamp created;

        private Timestamp modified;

        public Person(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public Timestamp getCreated() {
            return created;
        }

        public void setCreated(Timestamp created) {
            this.created = created;
        }

        public Timestamp getModified() {
            return modified;
        }

        public void setModified(Timestamp modified) {
            this.modified = modified;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Person person = (Person) o;

            if (id != person.id) {
                return false;
            }
            if (!firstName.equals(person.firstName)) {
                return false;
            }
            return lastName != null ? lastName.equals(person.lastName) : person.lastName == null;
        }

        @Override
        public int hashCode() {
            int result = id;
            result = 31 * result + firstName.hashCode();
            result = 31 * result + lastName.hashCode();
            result = 31 * result + (created != null ? created.hashCode() : 0);
            result = 31 * result + (modified != null ? modified.hashCode() : 0);
            return result;
        }
    }
}
