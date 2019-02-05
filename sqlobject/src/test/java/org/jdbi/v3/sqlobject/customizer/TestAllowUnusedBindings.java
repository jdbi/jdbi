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
package org.jdbi.v3.sqlobject.customizer;

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestAllowUnusedBindings {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());
    private UnusedBindingDao dao;

    @Before
    public void setUp() {
        dao = dbRule.getJdbi().onDemand(UnusedBindingDao.class);
    }

    @Test
    public void testAllowed() {
        assertThat(dao.allowed("42")).isTrue();
    }

    @Test
    public void testDisallowed() {
        dao.getHandle().getConfig(SqlStatements.class).setUnusedBindingAllowed(true);
        assertThatThrownBy(() -> dao.disallowed("43"))
            .isInstanceOf(UnableToCreateStatementException.class)
            .hasMessageContaining("named parameter");
    }

    @Test
    public void testUnannotated() {
        dbRule.getJdbi().getConfig(SqlStatements.class).setUnusedBindingAllowed(true);
        assertThat(dao.unannotated("42")).isTrue();
    }

    interface UnusedBindingDao extends SqlObject {
        @SqlQuery("select true")
        @AllowUnusedBindings
        boolean allowed(String id);

        @SqlQuery("select true")
        @AllowUnusedBindings(false)
        boolean disallowed(String id);

        @SqlQuery("select true")
        boolean unannotated(String id);
    }
}
