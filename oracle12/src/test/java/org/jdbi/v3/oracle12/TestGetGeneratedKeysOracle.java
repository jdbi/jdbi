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
package org.jdbi.v3.oracle12;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.tc.JdbiTestcontainersExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test uses an oracle instance in a testcontainer.
 */
@Tag("slow")
@Testcontainers
public class TestGetGeneratedKeysOracle {

    static final String CONTAINER_VERSION = "gvenzl/oracle-free:" + System.getProperty("oracle.container.version", "slim-faststart");

    @Container
    static OracleContainer oc = new OracleContainer(CONTAINER_VERSION);

    @RegisterExtension
    public JdbiExtension oracleExtension = JdbiTestcontainersExtension.instance(oc)
            .withPlugin(new SqlObjectPlugin());

    @BeforeEach
    public void beforeEach() {
        Handle handle = oracleExtension.getSharedHandle();
        handle.execute(
                "create sequence something_id_sequence INCREMENT BY 1 START WITH 100");
        handle.execute(
                "create table something (name varchar(200), id int, constraint something_id primary key (id))");
    }

    /**
     * Oracle needs to be queried by index and not id (like {@code DefaultGeneratedKeyMapper}
     * does).
     */
    public static class OracleGeneratedKeyMapper implements RowMapper<Long> {

        @Override
        public Long map(ResultSet r, StatementContext ctx) throws SQLException {
            return r.getLong(1);
        }
    }

    public interface DAO {

        @SqlUpdate("insert into something (name, id) values (:name, something_id_sequence.nextval)")
        @GetGeneratedKeys("id")
        long insert(@Bind("name") String name);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind long id);
    }

    @Test
    public void testGetGeneratedKeys() throws Exception {
        oracleExtension.getJdbi().useExtension(DAO.class, dao -> {
            Long fooId = dao.insert("Foo");
            long barId = dao.insert("Bar");

            assertThat(dao.findNameById(fooId)).isEqualTo("Foo");
            assertThat(dao.findNameById(barId)).isEqualTo("Bar");
        });
    }
}
