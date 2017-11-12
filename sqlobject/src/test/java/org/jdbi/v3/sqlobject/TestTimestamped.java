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

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Timestamped;
import org.jdbi.v3.sqlobject.customizer.internal.TimestampedConfig;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link Timestamped} annotation
 */
public class TestTimestamped {
    public PersonDAO personDAO;

    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    private MockClock mockClock = new MockClock();

    @Before
    public void beforeEach() {
        Jdbi jdbi = dbRule.getJdbi();
        jdbi.installPlugin(new SqlObjectPlugin());
        jdbi.getConfig(TimestampedConfig.class).setClock(mockClock);
        personDAO = jdbi.onDemand(PersonDAO.class);
        personDAO.createTable();
    }

    @Test
    public void shouldInsertCreatedAndModifiedFields() {
        Instant insertTimestamp = mockClock.instant();

        personDAO.insert(new Person(1, "John", "Phiri"));

        Person found = personDAO.get(1);

        assertThat(found)
            .extracting("id", "firstName", "lastName", "created", "modified")
            .containsExactly(1, "John", "Phiri", insertTimestamp, insertTimestamp);
    }

    @Test
    public void shouldAllowCustomTimestampParameter() {
        Instant insertTimestamp = mockClock.instant();

        personDAO.insertWithCustomTimestampFields(new Person(1, "John", "Phiri"));

        Person fetched = personDAO.get(1);

        assertThat(fetched)
            .extracting("id", "firstName", "lastName", "created", "modified")
            .containsExactly(1, "John", "Phiri", insertTimestamp, insertTimestamp);
    }

    @Test
    public void shouldUpdateModifiedTimestamp() {
        Instant insertTimestamp = mockClock.instant();

        personDAO.insert(new Person(3, "John", "Phiri"));

        Person created = personDAO.get(3);
        assertThat(created)
            .extracting("id", "firstName", "lastName", "created", "modified")
            .containsExactly(3, "John", "Phiri", insertTimestamp, insertTimestamp);

        Instant updateTimestamp = mockClock.advance(10, ChronoUnit.SECONDS);

        created.setLastName("Banda");
        personDAO.updatePerson(created);

        Person updated = personDAO.get(3);
        assertThat(updated)
            .extracting("id", "firstName", "lastName", "created", "modified")
            .containsExactly(3, "John", "Banda", insertTimestamp, updateTimestamp);
    }

    @RegisterBeanMapper(Person.class)
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

    /**
     * Person JavaBean for tests
     */
    public final static class Person {
        private int id;

        private String firstName;

        private String lastName;

        private Instant created;

        private Instant modified;

        public Person() {
        }

        public Person(int id, String firstName, String lastName) {
            this.id = id;
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

        public Instant getCreated() {
            return created;
        }

        public void setCreated(Instant created) {
            this.created = created;
        }

        public Instant getModified() {
            return modified;
        }

        public void setModified(Instant modified) {
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
