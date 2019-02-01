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
package org.jdbi.v3.postgres;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlCall;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.postgresql.util.PGobject;

import static org.assertj.core.api.Assertions.assertThat;

public class TestPostgresTypes {

    @ClassRule
    public static JdbiRule postgresDBRule = JdbiRule.embeddedPostgres();

    private static Jdbi jdbi;
    private Handle handle;

    @BeforeClass
    public static void beforeClass() {
        PostgresTypes.registerCustomType("foo_bar_type", FooBarPGType.class);

        jdbi = postgresDBRule.getJdbi();

        jdbi.installPlugin(new SqlObjectPlugin());
        jdbi.installPlugin(new PostgresPlugin());
    }

    @Before
    public void before() {
        handle = jdbi.open();
        handle.useTransaction(h -> {
            h.execute("drop table if exists postgres_custom_types");
            h.execute("create table postgres_custom_types(id integer not null, foo text, bar text, created_on timestamp)");

            // create custom type
            h.execute("drop function if exists get_foo_bars()");
            h.execute("drop function if exists get_foo_bar(integer)");
            h.execute("drop function if exists insert_foo_bar(foo_bar_type)");
            h.execute("drop function if exists insert_foo_bars(foo_bar_type[])");
            h.execute("drop type if exists foo_bar_type");

            h.execute("CREATE TYPE foo_bar_type AS (id integer, foo text, bar text);");

            //create functions using custom types
            h.execute("CREATE OR REPLACE FUNCTION get_foo_bars() RETURNS SETOF foo_bar_type AS \n"
                    + "$$ \n"
                    + "SELECT id, foo, bar FROM postgres_custom_types;\n"
                    + "$$ LANGUAGE sql;");

            h.execute("CREATE OR REPLACE FUNCTION get_foo_bar(aId integer) RETURNS foo_bar_type AS \n"
                    + "$$ \n"
                    + "SELECT id, foo, bar FROM postgres_custom_types WHERE id = aId;\n"
                    + "$$ LANGUAGE sql;");

            h.execute("CREATE OR REPLACE FUNCTION insert_foo_bar(aFooBar foo_bar_type) RETURNS void AS \n"
                    + "$$\n"
                    + "DECLARE\n"
                    + "\n"
                    + "BEGIN\n"
                    + "INSERT INTO postgres_custom_types(id, foo, bar, created_on) VALUES(aFooBar.id, aFooBar.foo, aFooBar.bar, current_timestamp);"
                    + "\n"
                    + "END;\n"
                    + "$$ LANGUAGE plpgsql;");

            h.execute("CREATE OR REPLACE FUNCTION insert_foo_bars(aFooBars foo_bar_type[]) RETURNS void AS \n"
                    + "$$\n"
                    + "DECLARE\n"
                    + "qFooBarType foo_bar_type;\n"
                    + "BEGIN\n"
                    + "FOREACH qFooBarType IN ARRAY aFooBars\n"
                    + "LOOP \n"
                    + "INSERT INTO postgres_custom_types(id, foo, bar, created_on) VALUES(qFooBarType.id, qFooBarType.foo, qFooBarType.bar, current_timestamp);"
                    + "END LOOP;\n"
                    + "END;\n"
                    + "$$ LANGUAGE plpgsql;");

            handle.execute("INSERT INTO postgres_custom_types(id, foo, bar, created_on) VALUES(1, 'foo1', 'bar1', current_timestamp)");
            handle.execute("INSERT INTO postgres_custom_types(id, foo, bar, created_on) VALUES(2, 'foo2', 'bar2', current_timestamp)");
        });
    }

    @After
    public void after() {
        handle.close();
    }

    @Test
    public void testReadViaFluentAPI() {
        FooBarPGType result = (FooBarPGType) handle.createQuery("SELECT get_foo_bar(1)")
                .map(new PGObjectColumnMapper())
                .findOnly();

        assertThat(result).isEqualTo(new FooBarPGType(1, "foo1", "bar1"));
    }

    @Test
    public void testReadListViaFluentAPI() {
        List<PGobject> result = handle.createQuery("SELECT get_foo_bars()")
                .map(new PGObjectColumnMapper())
                .list();

        assertThat(result).containsExactlyInAnyOrder(
            new FooBarPGType(1, "foo1", "bar1"),
            new FooBarPGType(2, "foo2", "bar2")
        );
    }

    @Test
    public void testWriteViaFluentAPI() {
        FooBarPGType fooBar3 = new FooBarPGType(3, "foo3", "bar3");

        handle.createCall("SELECT insert_foo_bar(:fooBar)")
                .bind("fooBar", fooBar3)
                .invoke();

        FooBarPGType result = (FooBarPGType) handle.createQuery("SELECT get_foo_bar(:id)")
                .bind("id", fooBar3.getId())
                .map(new PGObjectColumnMapper())
                .findOnly();

        assertThat(fooBar3).isEqualTo(result);
    }

    @Test
    public void testWriteArrayViaFluentAPI() {
        FooBarPGType fooBar5 = new FooBarPGType(5, "foo5", "bar5");
        FooBarPGType fooBar6 = new FooBarPGType(6, "foo6", "bar6");

        handle.createCall("SELECT insert_foo_bars(:fooBar)")
                .bind("fooBar", new FooBarPGType[]{fooBar5, fooBar6})
                .invoke();

        FooBarPGType result5 = (FooBarPGType) handle.createQuery("SELECT get_foo_bar(:id)")
                .bind("id", fooBar5.getId())
                .map(new PGObjectColumnMapper())
                .findOnly();

        FooBarPGType result6 = (FooBarPGType) handle.createQuery("SELECT get_foo_bar(:id)")
                .bind("id", fooBar6.getId())
                .map(new PGObjectColumnMapper())
                .findOnly();

        assertThat(fooBar5).isEqualTo(result5);
        assertThat(fooBar6).isEqualTo(result6);
    }

    @Test
    public void testReadViaObjectAPI() {
        PostgresCustomTypeDAO typeDAO = handle.attach(PostgresCustomTypeDAO.class);

        FooBarPGType result = (FooBarPGType) typeDAO.find(2);

        assertThat(result).isEqualTo(new FooBarPGType(2, "foo2", "bar2"));
}

    @Test
    public void testReadListViaObjectAPI() {
        PostgresCustomTypeDAO typeDAO = handle.attach(PostgresCustomTypeDAO.class);

        List<PGobject> result = typeDAO.getAllFooBars();

        assertThat(result).containsExactlyInAnyOrder(
            new FooBarPGType(1, "foo1", "bar1"),
            new FooBarPGType(2, "foo2", "bar2")
        );
    }

    @Test
    public void testWriteViaObjectAPI() {
        PostgresCustomTypeDAO typeDAO = handle.attach(PostgresCustomTypeDAO.class);
        FooBarPGType fooBar4 = new FooBarPGType(4, "foo4", "bar4");

        typeDAO.insertFooBar(fooBar4);
        FooBarPGType result = (FooBarPGType) typeDAO.find(fooBar4.getId());

        assertThat(fooBar4).isEqualTo(result);
    }

    @Test
    public void testWriteArrayViaObjectAPI() {
        PostgresCustomTypeDAO typeDAO = handle.attach(PostgresCustomTypeDAO.class);
        FooBarPGType fooBar7 = new FooBarPGType(7, "foo7", "bar7");
        FooBarPGType fooBar8 = new FooBarPGType(8, "foo8", "bar8");

        typeDAO.insertFooBars(new FooBarPGType[]{fooBar7, fooBar8});

        FooBarPGType result7 = (FooBarPGType) typeDAO.find(fooBar7.getId());
        FooBarPGType result8 = (FooBarPGType) typeDAO.find(fooBar8.getId());

        assertThat(fooBar7).isEqualTo(result7);
        assertThat(fooBar8).isEqualTo(result8);
    }

    public interface PostgresCustomTypeDAO {

        @SqlQuery("select get_foo_bars()")
        List<PGobject> getAllFooBars();

        @SqlQuery("select get_foo_bar(:id)")
        PGobject find(@Bind("id") int id);

        @SqlCall("select insert_foo_bar(:fooBar)")
        void insertFooBar(@Bind("fooBar") FooBarPGType foo);

        @SqlCall("select insert_foo_bars(:fooBars)")
        void insertFooBars(@Bind("fooBars") FooBarPGType[] foos);
    }
}
