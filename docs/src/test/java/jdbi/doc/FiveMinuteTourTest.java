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
package jdbi.doc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class FiveMinuteTourTest {
    private Jdbi jdbi;
    private Handle handle;

    @Before
    @SuppressWarnings("checkstyle:hiddenfield")
    public void setUp() {
        // tag::createJdbi[]
        // H2 in-memory database
        Jdbi jdbi = Jdbi.create("jdbc:h2:mem:test");
        // end::createJdbi[]

        // shared handle to keep database open
        this.jdbi = jdbi;
        this.handle = jdbi.open();

        // tag::useHandle[]
        jdbi.useHandle(handle -> {
            handle.execute("create table contacts (id int primary key, name varchar(100))");
            handle.execute("insert into contacts (id, name) values (?, ?)", 1, "Alice");
            handle.execute("insert into contacts (id, name) values (?, ?)", 2, "Bob");
        });
        // end::useHandle[]

        // tag::withHandle[]
        List<String> names = jdbi.withHandle(handle ->
            handle.createQuery("select name from contacts")
                  .mapTo(String.class)
                  .list());
        assertThat(names).contains("Alice", "Bob");
        // end::withHandle[]
    }

    @After
    public void tearDown() {
        handle.close();
    }

    @Test
    public void tryWithResources() {
        // tag::openHandle[]
        try (Handle handle = jdbi.open()) {
            handle.execute("insert into contacts (id, name) values (?, ?)", 3, "Chuck");
        }
        // end::openHandle[]
    }

    // tag::defineCustomMapper[]
    public final class Contact {
        final int id;
        final String name;

        Contact(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public class ContactMapper implements RowMapper<Contact> {
        @Override
        public Contact map(ResultSet r, StatementContext ctx) throws SQLException {
            return new Contact(r.getInt("id"), r.getString("name"));
        }
    }
    // end::defineCustomMapper[]

    @Test
    public void useCustomMapper() {
        // tag::useCustomMapper[]
        List<Contact> contacts = handle.createQuery("select * from contacts")
                                       .map(new ContactMapper())
                                       .list();
        assertThat(contacts).extracting("id", "name")
                            .contains(tuple(1, "Alice"),
                                      tuple(2, "Bob"));
        // end::useCustomMapper[]
    }

    @Test
    public void registerCustomMapper() {
        // tag::registerCustomMapper[]
        handle.registerRowMapper(new ContactMapper());

        List<Contact> contacts = handle.createQuery("select * from contacts")
                                       .mapTo(Contact.class)
                                       .list();
        assertThat(contacts).extracting("id", "name")
                            .contains(tuple(1, "Alice"),
                                      tuple(2, "Bob"));
        // end::registerCustomMapper[]
    }

    @Test
    public void positionalParameters() {
        handle.registerRowMapper(new ContactMapper());
        // tag::positionalParameters[]
        handle.createUpdate("insert into contacts (id, name) values (?, ?)")
              .bind(0, 3)
              .bind(1, "Chuck")
              .execute();

        String name = handle.createQuery("select name from contacts where id = ?")
                            .bind(0, 3)
                            .mapTo(String.class)
                            .one();
        // end::positionalParameters[]
        assertThat(name).isEqualTo("Chuck");
    }

    @Test
    public void namedParameters() {
        handle.registerRowMapper(new ContactMapper());
        // tag::namedParameters[]
        handle.createUpdate("insert into contacts (id, name) values (:id, :name)")
              .bind("id", 3)
              .bind("name", "Chuck")
              .execute();

        String name = handle.createQuery("select name from contacts where id = :id")
                            .bind("id", 3)
                            .mapTo(String.class)
                            .one();
        // end::namedParameters[]
        assertThat(name).isEqualTo("Chuck");
    }
}
