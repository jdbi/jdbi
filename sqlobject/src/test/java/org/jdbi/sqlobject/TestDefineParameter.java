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
package org.jdbi.sqlobject;

import org.jdbi.core.Handle;
import org.jdbi.core.Something;
import org.jdbi.core.mapper.SomethingMapper;
import org.jdbi.sqlobject.config.RegisterRowMapper;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.customizer.BindBean;
import org.jdbi.sqlobject.customizer.Define;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestDefineParameter {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
    }

    @Test
    public void testDefineParameter() {
        handle.execute("create table stuff (id identity primary key, name varchar(50))");
        handle.execute("create table junk (id identity primary key, name varchar(50))");

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
