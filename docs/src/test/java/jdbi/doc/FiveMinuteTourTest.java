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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.jdbi.v3.Handle;
import org.jdbi.v3.Jdbi;
import org.jdbi.v3.StatementContext;
import org.jdbi.v3.mapper.RowMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FiveMinuteTourTest {
    private Jdbi dbi;
    private Handle handle;

    @Before
    public void setUp() {
        // tag::createJdbi[]
        // H2 in-memory database
        Jdbi dbi = Jdbi.create("jdbc:h2:mem:test");
        // end::createJdbi[]

        // shared handle to keep database open
        this.dbi = dbi;
        this.handle = dbi.open();

        // tag::useHandle[]
        dbi.useHandle(handle -> {
            handle.execute("create table something (id int primary key, name varchar(100))");
            handle.insert("insert into something (id, name) values (?, ?)", 1, "Alice");
            handle.insert("insert into something (id, name) values (?, ?)", 2, "Bob");

            List<String> names = handle.createQuery("select name from something")
                                       .mapTo(String.class)
                                       .list();
            assertThat(names).contains("Alice", "Bob");
        });
        // end::useHandle[]
    }

    @After
    public void tearDown() {
        handle.close();
    }

    @Test
    public void tryWithResources() {
        // tag::openHandle[]
        try (Handle handle = dbi.open()) {
            // do stuff
        }
        // end::openHandle[]
    }

    // tag::defineCustomMapper[]
    public final class Something {
        final int id;
        final String name;

        Something(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public class SomethingMapper implements RowMapper<Something> {
        @Override
        public Something map(ResultSet r, StatementContext ctx) throws SQLException {
            return new Something(r.getInt("id"), r.getString("name"));
        }
    }
    // end::defineCustomMapper[]

    @Test
    public void useCustomMapper() {
        // tag::useCustomMapper[]
        List<Something> things = handle.createQuery("select * from something")
                                       .map(new SomethingMapper())
                                       .list();
        assertThat(things).extracting("id", "name")
                          .contains(tuple(1, "Alice"),
                                    tuple(2, "Bob"));
        // end::useCustomMapper[]
    }

    @Test
    public void registerCustomMapper() {
        // tag::registerCustomMapper[]
        handle.registerRowMapper(new SomethingMapper());

        List<Something> things = handle.createQuery("select * from something")
                                       .mapTo(Something.class)
                                       .list();
        assertThat(things).extracting("id", "name")
                          .contains(tuple(1, "Alice"),
                                    tuple(2, "Bob"));
        // end::registerCustomMapper[]
    }

    @Test
    public void namedParameters() {
        handle.registerRowMapper(new SomethingMapper());
        // tag::namedParameters[]
        Something thing = handle.createQuery("select * from something where id = :id")
                                .bind("id", 1)
                                .mapTo(Something.class)
                                .findOnly();
        assertThat(thing).extracting("id", "name")
                         .contains(1, "Alice");
        // end::namedParameters[]
    }
}
