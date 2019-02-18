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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.generic.GenericType;
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
import org.postgresql.geometric.PGbox;
import org.postgresql.geometric.PGcircle;
import org.postgresql.geometric.PGline;
import org.postgresql.geometric.PGlseg;
import org.postgresql.geometric.PGpath;
import org.postgresql.geometric.PGpoint;
import org.postgresql.geometric.PGpolygon;
import org.postgresql.util.PGInterval;
import org.postgresql.util.PGmoney;

import static org.assertj.core.api.Assertions.assertThat;

public class TestPostgresTypes {

    @ClassRule
    public static JdbiRule postgresDBRule = PostgresDbRule.rule(builder -> {
        // We need to force the locale for the 'testReadWriteMoney' test
        final String locale;

        if (SystemUtils.IS_OS_WINDOWS) {
            locale = "English_United States";
        } else {
            locale = "en_US";
        }

        builder.setLocaleConfig("locale", locale);
    });

    private static Jdbi jdbi;
    private Handle handle;

    @BeforeClass
    public static void beforeClass() {
        jdbi = postgresDBRule.getJdbi()
            .installPlugin(new SqlObjectPlugin())
            .installPlugin(new PostgresPlugin())
            .configure(PostgresTypes.class, pt -> pt.registerCustomType(FooBarPGType.class, "foo_bar_type"));
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
        FooBarPGType result = handle.createQuery("SELECT get_foo_bar(1)")
                .mapTo(FooBarPGType.class)
                .findOnly();

        assertThat(result).isEqualTo(new FooBarPGType(1, "foo1", "bar1"));
    }

    @Test
    public void testReadListViaFluentAPI() {
        List<FooBarPGType> result = handle.createQuery("SELECT get_foo_bars()")
                .mapTo(FooBarPGType.class)
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

        FooBarPGType result = handle.createQuery("SELECT get_foo_bar(:id)")
                .bind("id", fooBar3.getId())
                .mapTo(FooBarPGType.class)
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

        FooBarPGType result5 = handle.createQuery("SELECT get_foo_bar(:id)")
                .bind("id", fooBar5.getId())
                .mapTo(FooBarPGType.class)
                .findOnly();

        FooBarPGType result6 = handle.createQuery("SELECT get_foo_bar(:id)")
                .bind("id", fooBar6.getId())
                .mapTo(FooBarPGType.class)
                .findOnly();

        assertThat(fooBar5).isEqualTo(result5);
        assertThat(fooBar6).isEqualTo(result6);
    }

    @Test
    public void testReadViaObjectAPI() {
        PostgresCustomTypeDAO typeDAO = handle.attach(PostgresCustomTypeDAO.class);

        FooBarPGType result = typeDAO.find(2);

        assertThat(result).isEqualTo(new FooBarPGType(2, "foo2", "bar2"));
    }

    @Test
    public void testReadListViaObjectAPI() {
        PostgresCustomTypeDAO typeDAO = handle.attach(PostgresCustomTypeDAO.class);

        List<FooBarPGType> result = typeDAO.getAllFooBars();

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
        FooBarPGType result = typeDAO.find(fooBar4.getId());

        assertThat(fooBar4).isEqualTo(result);
    }

    @Test
    public void testWriteArrayViaObjectAPI() {
        PostgresCustomTypeDAO typeDAO = handle.attach(PostgresCustomTypeDAO.class);
        FooBarPGType fooBar7 = new FooBarPGType(7, "foo7", "bar7");
        FooBarPGType fooBar8 = new FooBarPGType(8, "foo8", "bar8");

        typeDAO.insertFooBarsArray(new FooBarPGType[]{fooBar7, fooBar8});

        FooBarPGType result7 = typeDAO.find(fooBar7.getId());
        FooBarPGType result8 = typeDAO.find(fooBar8.getId());

        assertThat(fooBar7).isEqualTo(result7);
        assertThat(fooBar8).isEqualTo(result8);
    }

    @Test
    public void testBindListAsArrayViaFluentAPI() {
        List<FooBarPGType> foos = new ArrayList<>();
        foos.add(new FooBarPGType(9, "foo9", "bar9"));
        foos.add(new FooBarPGType(10, "foo10", "bar10"));

        handle.createCall("SELECT insert_foo_bars(:fooBar)")
                .bindByType("fooBar", foos, new GenericType<List<FooBarPGType>>() {})
                .invoke();

        assertThat(handle.createQuery("SELECT get_foo_bars()")
                .mapTo(FooBarPGType.class)
                .list())
                .containsExactlyInAnyOrder(new FooBarPGType(1, "foo1", "bar1"),
                                           new FooBarPGType(2, "foo2", "bar2"),
                                           new FooBarPGType(9, "foo9", "bar9"),
                                           new FooBarPGType(10, "foo10", "bar10"));
    }

    @Test
    public void testBindListAsArrayViaObjectAPI() {
        PostgresCustomTypeDAO typeDAO = handle.attach(PostgresCustomTypeDAO.class);

        List<FooBarPGType> foos = new ArrayList<>();
        foos.add(new FooBarPGType(11, "foo11", "bar11"));
        foos.add(new FooBarPGType(12, "foo12", "bar12"));

        typeDAO.insertFooBars(foos);

        assertThat(typeDAO.getAllFooBars())
                .containsExactlyInAnyOrder(new FooBarPGType(1, "foo1", "bar1"),
                                           new FooBarPGType(2, "foo2", "bar2"),
                                           new FooBarPGType(11, "foo11", "bar11"),
                                           new FooBarPGType(12, "foo12", "bar12"));
    }

    @Test
    public void testReadWriteBox() {
        assertThat(handle.select("select :box")
            .bind("box", new PGbox(1, 2, 3, 4))
            .mapTo(PGbox.class)
            .findOnly())
            .isEqualTo(new PGbox(1, 2, 3, 4));

        assertThat(handle.select("select :boxes")
            .bind("boxes", new PGbox[] {new PGbox(1, 2, 3, 4), new PGbox(5, 6, 7, 8)})
            .mapTo(PGbox[].class)
            .findOnly())
            .containsExactly(new PGbox(1, 2, 3, 4), new PGbox(5, 6, 7, 8));
    }

    @Test
    public void testReadWriteCircle() {
        assertThat(handle.select("select :circle")
            .bind("circle", new PGcircle(1, 2, 3))
            .mapTo(PGcircle.class)
            .findOnly())
            .isEqualTo(new PGcircle(1, 2, 3));

        assertThat(handle.select("select :circles")
            .bind("circles", new PGcircle[] {new PGcircle(1, 2, 3), new PGcircle(4, 5, 6)})
            .mapTo(PGcircle[].class)
            .findOnly())
            .containsExactly(new PGcircle(1, 2, 3), new PGcircle(4, 5, 6));
    }

    @Test
    public void testReadWriteInterval() {
        assertThat(handle.select("select :interval")
            .bind("interval", new PGInterval(1, 2, 3, 4, 5, 6))
            .mapTo(PGInterval.class)
            .findOnly())
            .isEqualTo(new PGInterval(1, 2, 3, 4, 5, 6));

        assertThat(handle.select("select :intervals")
            .bind("intervals", new PGInterval[] {new PGInterval(1, 2, 3, 4, 5, 6), new PGInterval(7, 8, 9, 10, 11, 12)})
            .mapTo(PGInterval[].class)
            .findOnly())
            .containsExactly(new PGInterval(1, 2, 3, 4, 5, 6), new PGInterval(7, 8, 9, 10, 11, 12));
    }

    @Test
    public void testReadWriteLine() {
        assertThat(handle.select("select :line")
            .bind("line", new PGline(1, 2, 3, 4))
            .mapTo(PGline.class)
            .findOnly())
            .isEqualTo(new PGline(1, 2, 3, 4));

        assertThat(handle.select("select :lines")
            .bind("lines", new PGline[] {new PGline(1, 2, 3, 4), new PGline(5, 6, 7, 8)})
            .mapTo(PGline[].class)
            .findOnly())
            .containsExactly(new PGline(1, 2, 3, 4), new PGline(5, 6, 7, 8));
    }

    @Test
    public void testReadWriteLseg() {
        assertThat(handle.select("select :lseg")
            .bind("lseg", new PGlseg(1, 2, 3, 4))
            .mapTo(PGlseg.class)
            .findOnly())
            .isEqualTo(new PGlseg(1, 2, 3, 4));

        assertThat(handle.select("select :lsegs")
            .bind("lsegs", new PGlseg[] {new PGlseg(1, 2, 3, 4), new PGlseg(5, 6, 7, 8)})
            .mapTo(PGlseg[].class)
            .findOnly())
            .containsExactly(new PGlseg(1, 2, 3, 4), new PGlseg(5, 6, 7, 8));
    }

    @Test
    public void testReadWriteMoney() {
        assertThat(handle.select("select :money")
            .bind("money", new PGmoney(1))
            .mapTo(PGmoney.class)
            .findOnly())
            .isEqualTo(new PGmoney(1));

        assertThat(handle.select("select :moneys")
            .bind("moneys", new PGmoney[] {new PGmoney(1), new PGmoney(2)})
            .mapTo(PGmoney[].class)
            .findOnly())
            .containsExactly(new PGmoney(1), new PGmoney(2));
    }

    @Test
    public void testReadWritePath() {
        assertThat(handle.select("select :path")
            .bind("path", new PGpath(new PGpoint[] {new PGpoint(1, 2), new PGpoint(3, 4), new PGpoint(5, 6)}, true))
            .mapTo(PGpath.class)
            .findOnly())
            .isEqualTo(new PGpath(new PGpoint[] {new PGpoint(1, 2), new PGpoint(3, 4), new PGpoint(5, 6)}, true));

        assertThat(handle.select("select :paths")
            .bind("paths", new PGpath[] {
                new PGpath(new PGpoint[] {new PGpoint(1, 2), new PGpoint(3, 4), new PGpoint(5, 6)}, true),
                new PGpath(new PGpoint[] {new PGpoint(7, 8), new PGpoint(9, 10), new PGpoint(11, 12)}, false)
            })
            .mapTo(PGpath[].class)
            .findOnly())
            .containsExactly(
                new PGpath(new PGpoint[] {new PGpoint(1, 2), new PGpoint(3, 4), new PGpoint(5, 6)}, true),
                new PGpath(new PGpoint[] {new PGpoint(7, 8), new PGpoint(9, 10), new PGpoint(11, 12)}, false)
            );
    }

    @Test
    public void testReadWritePoint() {
        assertThat(handle.select("select :point")
            .bind("point", new PGpoint(1, 2))
            .mapTo(PGpoint.class)
            .findOnly())
            .isEqualTo(new PGpoint(1, 2));

        assertThat(handle.select("select :points")
            .bind("points", new PGpoint[] {new PGpoint(1, 2), new PGpoint(3, 4), new PGpoint(5, 6)})
            .mapTo(PGpoint[].class)
            .findOnly())
            .containsExactly(new PGpoint(1, 2), new PGpoint(3, 4), new PGpoint(5, 6));
    }

    @Test
    public void testReadWritePolygon() {
        assertThat(handle.select("select :polygon")
            .bind("polygon", new PGpolygon(new PGpoint[] {new PGpoint(1, 2), new PGpoint(3, 4), new PGpoint(5, 6)}))
            .mapTo(PGpolygon.class)
            .findOnly())
            .isEqualTo(new PGpolygon(new PGpoint[] {new PGpoint(1, 2), new PGpoint(3, 4), new PGpoint(5, 6)}));

        assertThat(handle.select("select :polygons")
            .bind("polygons", new PGpolygon[] {
                new PGpolygon(new PGpoint[] {new PGpoint(1, 2), new PGpoint(3, 4), new PGpoint(5, 6)}),
                new PGpolygon(new PGpoint[] {new PGpoint(7, 8), new PGpoint(9, 10), new PGpoint(11, 12)})
            })
            .mapTo(PGpolygon[].class)
            .findOnly())
            .containsExactly(
                new PGpolygon(new PGpoint[] {new PGpoint(1, 2), new PGpoint(3, 4), new PGpoint(5, 6)}),
                new PGpolygon(new PGpoint[] {new PGpoint(7, 8), new PGpoint(9, 10), new PGpoint(11, 12)})
            );
    }

    public interface PostgresCustomTypeDAO {

        @SqlQuery("select get_foo_bars()")
        List<FooBarPGType> getAllFooBars();

        @SqlQuery("select get_foo_bar(:id)")
        FooBarPGType find(@Bind("id") int id);

        @SqlCall("select insert_foo_bar(:fooBar)")
        void insertFooBar(@Bind("fooBar") FooBarPGType foo);

        @SqlCall("select insert_foo_bars(:fooBars)")
        void insertFooBarsArray(@Bind("fooBars") FooBarPGType[] foos);

        @SqlCall("select insert_foo_bars(:fooBars)")
        void insertFooBars(@Bind("fooBars") List<FooBarPGType> foos);
    }
}
