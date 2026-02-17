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

import java.util.List;

import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.Something;
import org.jdbi.core.statement.UnableToCreateStatementException;
import org.jdbi.sqlobject.customizer.BindBean;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestReentrancy {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    private Jdbi jdbi;

    @BeforeEach
    public void setUp() {
        this.jdbi = h2Extension.getJdbi();
    }

    private interface TheBasics extends SqlObject {

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        int insert(@BindBean Something something);

        @SqlQuery("select count(1) from something")
        int count();
    }

    @Test
    public void testGetHandleProvidesSeperateHandle() {
        final TheBasics dao = jdbi.onDemand(TheBasics.class);
        Handle h = dao.getHandle();

        assertThatThrownBy(() -> h.execute("insert into something (id, name) values (1, 'Stephen')"))
            .isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    public void testHandleReentrant() {
        final TheBasics dao = jdbi.onDemand(TheBasics.class);

        dao.withHandle(handle1 -> {
            dao.insert(new Something(7, "Martin"));

            handle1.createQuery("SELECT 1").mapToMap().list();

            return null;
        });

        assertThat(dao.count()).isOne();
    }

    @Test
    public void testTxnReentrant() {
        final TheBasics dao = jdbi.onDemand(TheBasics.class);

        dao.withHandle(handle1 -> {
            handle1.useTransaction(h -> {
                dao.insert(new Something(1, "x"));

                List<String> rs = h.createQuery("select name from something where id = 1")
                        .mapTo(String.class)
                        .list();
                assertThat(rs).hasSize(1);

                h.createQuery("SELECT 1").mapTo(int.class).list();
            });

            return null;
        });
    }
}
