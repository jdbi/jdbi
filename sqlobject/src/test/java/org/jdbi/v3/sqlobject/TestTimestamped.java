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
package org.jdbi.v3.sqlobject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import org.jdbi.v3.core.Time;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Timestamped;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link Timestamped} annotation
 */
public class TestTimestamped {
    private static final ZonedDateTime T0 = LocalDate.of(2018, Month.JANUARY, 1).atTime(LocalTime.of(12, 0, 0)).atZone(ZoneOffset.UTC);
    private static final Timestamp T0_TIMESTAMP = Timestamp.from(T0.toInstant());
    @Rule
    public JdbiRule dbRule = JdbiRule.h2().withPlugin(new SqlObjectPlugin());
    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();
    @Mock
    private Clock clock;

    private PersonDAO personDAO;
    private VerifyingLogger logger;

    @Before
    public void before() {
        when(clock.instant()).thenReturn(T0.toInstant());
        when(clock.getZone()).thenReturn(T0.getZone());
        dbRule.getJdbi().getConfig(Time.class).setClock(clock);

        personDAO = dbRule.getJdbi().onDemand(PersonDAO.class);
        personDAO.createTable();
    }

    @Test
    public void shouldInsertCreatedAndModifiedFields() {
        Person p = new Person("John", "Phiri");
        p.setId(1);

        logNextTimestamp("now");
        personDAO.insert(p);
        verifyLastTimestamp(T0);

        Person inserted = personDAO.get(1);
        assertThat(inserted.getCreated()).isEqualTo(T0_TIMESTAMP);
        assertThat(inserted.getModified()).isEqualTo(T0_TIMESTAMP);
    }

    @Test
    public void shouldAllowCustomTimestampParameter() {
        Person p = new Person("John", "Phiri");
        p.setId(1);

        logNextTimestamp("createdAt");
        personDAO.insertWithCustomTimestampFields(p);
        verifyLastTimestamp(T0);

        Person inserted = personDAO.get(1);
        assertThat(p.getFirstName()).isEqualTo(inserted.getFirstName());
        assertThat(p.getLastName()).isEqualTo(inserted.getLastName());
        assertThat(inserted.getCreated()).isEqualTo(T0_TIMESTAMP);
        assertThat(inserted.getModified()).isEqualTo(T0_TIMESTAMP);
    }

    @Test
    public void shouldUpdateModifiedTimestamp() {

        Person p = new Person("John", "Phiri");
        p.setId(3);

        logNextTimestamp("now");
        personDAO.insert(p);
        verifyLastTimestamp(T0);

        when(clock.instant()).thenReturn(T0.plusSeconds(10).toInstant());

        Person personAfterCreate = personDAO.get(3);
        personAfterCreate.setLastName("Banda");

        personDAO.updatePerson(personAfterCreate);
        verifyLastTimestamp(T0.plusSeconds(10));

        Person personAfterUpdate = personDAO.get(3);

        assertThat(personAfterUpdate.getLastName()).isEqualToIgnoringCase("Banda");
        assertThat(personAfterUpdate.getCreated())
            .isEqualTo(T0_TIMESTAMP)
            .isEqualTo(personAfterCreate.getCreated());

        assertThat(personAfterUpdate.getModified())
            .isEqualTo(Timestamp.from(T0.plusSeconds(10).toInstant()));
    }

    private void logNextTimestamp(String name) {
        logger = new VerifyingLogger(name);
        dbRule.getJdbi().setSqlLogger(logger);
    }

    private void verifyLastTimestamp(ZonedDateTime expected) {
        assertThat(logger.found).isEqualTo(expected.toString());
    }

    // This is one way we can get the binding information of the executed query
    private static class VerifyingLogger implements SqlLogger {
        private final String name;
        private String found;

        private VerifyingLogger(String name) {
            this.name = name;
        }

        @Override
        public void logBeforeExecution(StatementContext context) {
            context.getBinding().findForName(name, context)
                // because it's an Argument
                .ifPresent(v -> found = v.toString());
        }
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

            Person other = (Person) o;
            return Objects.equals(id, other.id)
                && Objects.equals(firstName, other.firstName)
                && Objects.equals(lastName, other.lastName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, firstName, lastName, created, modified);
        }
    }
}
