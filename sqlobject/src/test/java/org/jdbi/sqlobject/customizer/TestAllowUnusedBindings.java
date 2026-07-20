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
package org.jdbi.sqlobject.customizer;

import org.jdbi.core.Handle;
import org.jdbi.core.statement.SqlStatements;
import org.jdbi.core.statement.UnableToCreateStatementException;
import org.jdbi.sqlobject.SqlObject;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestAllowUnusedBindings {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin());

    private UnusedBindingDao dao;

    @BeforeEach
    public void setUp() {
        dao = h2Extension.getJdbi().onDemand(UnusedBindingDao.class);
    }

    @Test
    public void testAllowed() {
        assertThat(dao.allowed("42")).isTrue();
    }

    @Test
    public void testDisallowed() {
        assertThatThrownBy(() -> {
            try (Handle handle = h2Extension.openWithConfig(
                    cfg -> cfg.configure(SqlStatements.class, c -> c.unusedBindingAllowed(true)))) {
                handle.attach(UnusedBindingDao.class).disallowed("43");
            }
        })
            .isInstanceOf(UnableToCreateStatementException.class)
            .hasMessageContaining("named parameter");
    }

    @Test
    public void testUnannotated() {
        final boolean result;
        try (Handle handle = h2Extension.openWithConfig(
            cfg -> cfg.configure(SqlStatements.class, c -> c.unusedBindingAllowed(true)))) {
            result = handle.attach(UnusedBindingDao.class).unannotated("42");
        }
        assertThat(result).isTrue();
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
