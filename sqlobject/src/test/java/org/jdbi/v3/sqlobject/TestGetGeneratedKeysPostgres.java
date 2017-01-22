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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.rule.PgDatabaseRule;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestGetGeneratedKeysPostgres
{
    @Rule
    public PgDatabaseRule dbRule = new PgDatabaseRule().withPlugin(new SqlObjectPlugin());

    @Before
    public void setUp() throws Exception {
        dbRule.getJdbi().useHandle(handle -> {
            handle.execute("create sequence id_sequence INCREMENT 1 START WITH 100");
            handle.execute("create table if not exists something (name text, id int DEFAULT nextval('id_sequence'), CONSTRAINT something_id PRIMARY KEY ( id ));");
        });
    }

    @After
    public void tearDown() throws Exception {
        dbRule.getJdbi().useHandle(handle -> {
            handle.execute("drop table something");
            handle.execute("drop sequence id_sequence");
        });
    }

    public interface DAO {
        @SqlUpdate("insert into something (name, id) values (:name, nextval('id_sequence'))")
        @GetGeneratedKeys("id")
        long insert(@Bind("name") String name);

        @SqlBatch("insert into something (name) values (:name)")
        @GetGeneratedKeys("id")
        int[] insert(@Bind("name") List<String> names);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind long id);
    }

    @Test
    public void testFoo() throws Exception {
        dbRule.getJdbi().useExtension(DAO.class, dao -> {
            long brian_id = dao.insert("Brian");
            long keith_id = dao.insert("Keith");

            assertThat(dao.findNameById(brian_id)).isEqualTo("Brian");
            assertThat(dao.findNameById(keith_id)).isEqualTo("Keith");
        });
    }

    @Test
    public void testBatch() throws Exception {
        dbRule.getJdbi().useExtension(DAO.class, dao -> {
            int[] ids = dao.insert(Arrays.asList("Burt", "Macklin"));
            assertThat(dao.findNameById(ids[0])).isEqualTo("Burt");
            assertThat(dao.findNameById(ids[1])).isEqualTo("Macklin");
        });
    }

    @Test
    public void testUpdateMultipleGeneratedKeys() throws Exception {
        dbRule.getJdbi().useHandle(handle -> {
            handle.execute("drop table something");
            handle.execute("create table something (id serial, name varchar(50), created_on timestamp default now())");

            GetMultipleGeneratedKeysDao dao = handle.attach(GetMultipleGeneratedKeysDao.class);

            IdCreateTime result = dao.insert("foo");
            assertThat(result.id).isEqualTo(1);
            assertThat(result.createdOn).isNotNull();
        });
    }

    @Test
    public void testBatchMultipleGeneratedKeys() throws Exception {
        dbRule.getJdbi().useHandle(handle -> {
            handle.execute("drop table something");
            handle.execute("create table something (id serial, name varchar(50), created_on timestamp default now())");

            GetMultipleGeneratedKeysDao dao = handle.attach(GetMultipleGeneratedKeysDao.class);

            List<IdCreateTime> results = dao.insertBatch("foo", "bar");
            assertThat(results).extracting(ic -> ic.id).containsExactly(1L, 2L);
            assertThat(results).extracting(ic -> ic.createdOn).hasSize(2).doesNotContainNull();
        });
    }

    public interface GetMultipleGeneratedKeysDao {
        @SqlUpdate("insert into something(name) values (:name)")
        @GetGeneratedKeys
        @UseRowMapper(IdCreateTimeMapper.class)
        IdCreateTime insert(String name);

        @SqlBatch("insert into something(name) values (:name)")
        @GetGeneratedKeys
        @UseRowMapper(IdCreateTimeMapper.class)
        List<IdCreateTime> insertBatch(String... name);
    }

    public static class IdCreateTimeMapper implements RowMapper<IdCreateTime> {
        @Override
        public IdCreateTime map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new IdCreateTime(
                    rs.getLong("id"),
                    rs.getTimestamp("created_on").toInstant().atOffset(ZoneOffset.UTC));
        }
    }

    public static class IdCreateTime {
        final long id;
        final OffsetDateTime createdOn;

        public IdCreateTime(long id, OffsetDateTime createdOn) {
            this.id = id;
            this.createdOn = createdOn;
        }
    }
}
