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
package org.jdbi.v3.oracle;


import static org.assertj.core.api.Assertions.assertThat;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Rule;
import org.junit.Test;

/**
 * This test assumes an instance of Oracle database called 'test' is
 * running on localhost port 1521.
 *
 * @author Ricco Førgaard mailto:ricco@vimond.com
 * @since 2014-10-18
 */
public class TestGetGeneratedKeysOracle {
    @Rule
    public OracleDatabaseRule dbRule = new OracleDatabaseRule().withPlugin(new SqlObjectPlugin());

    /**
     * Oracle needs to be queried by index and not id (like
     * {@code DefaultGeneratedKeyMapper} does).
     */
    public static class OracleGeneratedKeyMapper implements RowMapper<Long> {

        @Override
        public Long map(ResultSet r, StatementContext ctx) throws SQLException {
            return r.getLong(1);
        }
    }

    public interface DAO {
        @SqlUpdate("insert into something (name, id) values (:name, something_id_sequence.nextval)")
        @GetGeneratedKeys(columnName = "id", value = OracleGeneratedKeyMapper.class)
        long insert(@Bind("name") String name);

        @SqlQuery("select name from something where id = :it")
        String findNameById(@Bind long id);
    }

    @Test
    public void testGetGeneratedKeys() throws Exception {
        dbRule.getJdbi().useExtension(DAO.class, dao -> {
            Long fooId = dao.insert("Foo");
            long barId = dao.insert("Bar");

            assertThat(dao.findNameById(fooId)).isEqualTo("Foo");
            assertThat(dao.findNameById(barId)).isEqualTo("Bar");
        });
    }
}
