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

import static org.assertj.core.api.Assertions.assertThat;

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestDefineParameter {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @Before
    public void setUp() throws Exception {
        handle = dbRule.getSharedHandle();
    }

    @Test
    public void testDefineParameter() throws Exception {
        handle.execute("create table stuff (id identity primary key, name varchar(50))");
        handle.execute("create table junk  (id identity primary key, name varchar(50))");

        HoneyBadger badass = handle.attach(HoneyBadger.class);

        Something ted = new Something(1, "Ted");
        Something fred = new Something(2, "Fred");

        badass.insert("stuff", ted);
        badass.insert("junk", fred);

        assertThat(badass.findById("stuff", 1)).isEqualTo(ted);
        assertThat(badass.findById("junk", 1)).isNull();

        assertThat(badass.findById("stuff", 2)).isNull();
        assertThat(badass.findById("junk", 2)).isEqualTo(fred);
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface HoneyBadger {
        @SqlUpdate("insert into <table> (id, name) values (:id, :name)")
        void insert(@Define("table") String ermahgerd, @BindBean Something s);

        @SqlQuery("select id, name from <table> where id = :id")
        Something findById(@Define String table, @Bind("id") long id);
    }
}
