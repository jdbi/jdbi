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

import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestMistypedNamedParameter {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    private Dao dao;

    @BeforeEach
    public void setUp() {
        dao = h2Extension.getSharedHandle().attach(Dao.class);
        h2Extension.getSharedHandle().useTransaction(h -> {
            h.execute("insert into something (id, name) values (1, 'Alice')");
            h.execute("insert into something (id, name) values (2, 'Bob')");
            h.execute("insert into something (id, name) values (3, 'Charles')");
        });
    }

    @Test
    public void testWarnAboutUnmatchedBinding() {
        assertThatThrownBy(() -> dao.deleteSomething(2))
            .isInstanceOf(UnableToCreateStatementException.class)
            .hasMessageStartingWith("Superfluous named parameters provided while the query declares none");
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface Dao {

        @SqlUpdate("delete from something where id = id")
        void deleteSomething(@Bind("id") int id);
    }
}
