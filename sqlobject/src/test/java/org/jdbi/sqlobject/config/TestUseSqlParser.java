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
package org.jdbi.sqlobject.config;

import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.Something;
import org.jdbi.core.mapper.SomethingMapper;
import org.jdbi.core.statement.ColonPrefixSqlParser;
import org.jdbi.core.statement.HashPrefixSqlParser;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.customizer.BindBean;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit5.JdbiExtension;
import org.jdbi.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestUseSqlParser {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @BeforeEach
    public void setUp() {
        Jdbi db = h2Extension.getJdbi();

        // this is the default, but be explicit for sake of clarity in test
        db.setSqlParser(new ColonPrefixSqlParser());
        handle = db.open();
    }

    @AfterEach
    public void tearDown() {
        handle.close();
    }

    @Test
    public void testOnClass() {
        // test will raise exceptions if SQL is bogus -- if it uses the colon prefix form

        HashedOnClass h = handle.attach(HashedOnClass.class);
        h.insert(new Something(1, "Joy"));
        Something s = h.findById(1);
        assertThat(s.getName()).isEqualTo("Joy");
    }

    @Test
    public void testOnMethod() {
        // test will raise exceptions if SQL is bogus -- if it uses the colon prefix form

        HashedOnMethod h = handle.attach(HashedOnMethod.class);
        h.insert(new Something(1, "Joy"));
        Something s = h.findById(1);
        assertThat(s.getName()).isEqualTo("Joy");
    }

    @UseSqlParser(HashPrefixSqlParser.class)
    @RegisterRowMapper(SomethingMapper.class)
    public interface HashedOnClass {

        @SqlUpdate("insert into something (id, name) values (#id, #name)")
        void insert(@BindBean Something s);

        @SqlQuery("select id, name from something where id = #id")
        Something findById(@Bind("id") int id);
    }

    public interface HashedOnMethod {

        @UseSqlParser(HashPrefixSqlParser.class)
        @RegisterRowMapper(SomethingMapper.class)
        @SqlUpdate("insert into something (id, name) values (#id, #name)")
        void insert(@BindBean Something s);

        @UseSqlParser(HashPrefixSqlParser.class)
        @RegisterRowMapper(SomethingMapper.class)
        @SqlQuery("select id, name from something where id = #id")
        Something findById(@Bind("id") int id);
    }
}
