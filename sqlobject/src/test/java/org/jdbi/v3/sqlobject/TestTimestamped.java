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

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.TimingCollector;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Timestamped;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link Timestamped} annotation
 */
public class TestTimestamped {
    public PersonDAO personDAO;

    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    @Before
    public void beforeEach() {
        dbRule.getJdbi().installPlugin(new SqlObjectPlugin());
        personDAO = dbRule.getJdbi().onDemand(PersonDAO.class);
        personDAO.createTable();
    }

    @Test
    public void shouldInsertCreatedAndModifiedFields() {
        // This is one way we can get the binding information of the executed query
        dbRule.getJdbi().setTimingCollector((l, statementContext) -> {
            assertThat(statementContext.getBinding().findForName("now")).isPresent();
        });

        Person p = new Person("John", "Phiri");
        p.setId(1);
        personDAO.insert(p);

        // Clear the timing colletor
        dbRule.getJdbi().setTimingCollector(TimingCollector.NOP_TIMING_COLLECTOR);

        Person found = personDAO.get(1);

        assertThat(found.getCreated()).isNotNull();
        assertThat(found.getModified()).isNotNull();
    }

    @Test
    public void shouldAllowCustomTimestampParameter() {
        LocalDateTime timeBefore = LocalDateTime.now().minusMinutes(1);

        Person p = new Person("John", "Phiri");
        p.setId(1);
        dbRule.getJdbi().setTimingCollector((l, statementContext) -> {
            assertThat(statementContext.getBinding().findForName("createdAt")).isPresent();
        });


        personDAO.insertWithCustomTimestampFields(p);

        dbRule.getJdbi().setTimingCollector(TimingCollector.NOP_TIMING_COLLECTOR);

        Person fetched = personDAO.get(1);

        assertThat(p.getFirstName()).isEqualTo(fetched.getFirstName());
        assertThat(p.getLastName()).isEqualTo(fetched.getLastName());
        assertThat(fetched.getCreated()).isNotNull();
        assertThat(fetched.getModified()).isNotNull();

        assertThat(fetched.getCreated()).isEqualTo(fetched.getModified());

        assertThat(timeBefore).isBefore(fetched.getCreated().toLocalDateTime());
    }

    @Test
    public void shouldUpdateModifiedTimestamp() {
        Person p = new Person("John", "Phiri");

        p.setId(3);

        dbRule.getJdbi().setTimingCollector((l, statementContext) -> {
            assertThat(statementContext.getBinding().findForName("now")).isPresent();
        });

        personDAO.insert(p);

        dbRule.getJdbi().setTimingCollector(TimingCollector.NOP_TIMING_COLLECTOR);

        Person personAfterCreate = personDAO.get(3);

        personAfterCreate.setLastName("Banda");

        personDAO.updatePerson(personAfterCreate);

        Person personAfterUpdate = personDAO.get(3);

        assertThat(personAfterUpdate.getLastName()).isEqualToIgnoringCase("Banda");

        assertThat(personAfterUpdate.getCreated()).isEqualTo(personAfterCreate.getCreated());

        assertThat(personAfterUpdate.getModified()).isAfter(personAfterCreate.getModified());
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

    public final static class PersonRowMapper implements RowMapper<Person> {

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
    public final static class Person {
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
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Person person = (Person) o;

            if (id != person.id) return false;
            if (!firstName.equals(person.firstName)) return false;
            return lastName != null ? lastName.equals(person.lastName) : person.lastName== null;
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
