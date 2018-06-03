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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.rule.PgDatabaseRule;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestGetGeneratedKeysPostgres {
    @Rule
    public PgDatabaseRule dbRule = new PgDatabaseRule().withPlugin(new SqlObjectPlugin());

    @Test
    public void testFoo() throws Exception {
        dbRule.getJdbi().useExtension(DAO.class, dao -> {
            dao.createSequence();
            dao.createTable();

            long brianId = dao.insert("Brian");
            long keithId = dao.insert("Keith");

            assertThat(dao.findNameById(brianId)).isEqualTo("Brian");
            assertThat(dao.findNameById(keithId)).isEqualTo("Keith");
        });
    }

    @Test
    public void testBatch() throws Exception {
        dbRule.getJdbi().useExtension(DAO.class, dao -> {
            dao.createSequence();
            dao.createTable();

            int[] ids = dao.insert(Arrays.asList("Burt", "Macklin"));

            assertThat(dao.findNameById(ids[0])).isEqualTo("Burt");
            assertThat(dao.findNameById(ids[1])).isEqualTo("Macklin");
        });
    }

    @Test
    public void testUseRowMapperUpdate() throws Exception {
        dbRule.getJdbi().useExtension(UseRowMapperDao.class, dao -> {
            dao.createTable();

            IdCreateTime result = dao.insert("foo");

            assertThat(result.id).isEqualTo(1);
            assertThat(result.createdOn).isNotNull();
        });
    }

    @Test
    public void testUseRowMapperBatch() throws Exception {
        dbRule.getJdbi().useExtension(UseRowMapperDao.class, dao -> {
            dao.createTable();

            List<IdCreateTime> results = dao.insertBatch("foo", "bar");

            assertThat(results).extracting(ic -> ic.id).containsExactly(1L, 2L);
            assertThat(results).extracting(ic -> ic.createdOn).hasSize(2).doesNotContainNull();
        });
    }

    @Test
    public void testRegisterRowMapperUpdate() throws Exception {
        dbRule.getJdbi().useExtension(RegisterRowMapperDao.class, dao -> {
            dao.createTable();

            IdCreateTime result = dao.insert("foo");

            assertThat(result.id).isEqualTo(1);
            assertThat(result.createdOn).isNotNull();
        });
    }

    @Test
    public void testRegisterRowMapperBatch() throws Exception {
        dbRule.getJdbi().useExtension(RegisterRowMapperDao.class, dao -> {
            dao.createTable();

            List<IdCreateTime> results = dao.insertBatch("foo", "bar");

            assertThat(results).extracting(ic -> ic.id).containsExactly(1L, 2L);
            assertThat(results).extracting(ic -> ic.createdOn).hasSize(2).doesNotContainNull();
        });
    }

    public interface DAO {
        @SqlUpdate("create sequence id_sequence INCREMENT 1 START WITH 100")
        void createSequence();

        @SqlUpdate("create table if not exists something (name text, id int DEFAULT nextval('id_sequence'), CONSTRAINT something_id PRIMARY KEY (id))")
        void createTable();

        @SqlUpdate("insert into something (name, id) values (:name, nextval('id_sequence'))")
        @GetGeneratedKeys("id")
        long insert(@Bind("name") String name);

        @SqlBatch("insert into something (name) values (:name)")
        @GetGeneratedKeys("id")
        int[] insert(@Bind("name") List<String> names);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind long id);
    }

    public interface UseRowMapperDao {
        @SqlUpdate("create table something (id serial, name varchar(50), created_on timestamp default now())")
        void createTable();

        @SqlUpdate("insert into something(name) values (:name)")
        @GetGeneratedKeys({"id", "created_on"})
        @UseRowMapper(IdCreateTimeMapper.class)
        IdCreateTime insert(String name);

        @SqlBatch("insert into something(name) values (:name)")
        @GetGeneratedKeys({"id", "created_on"})
        @UseRowMapper(IdCreateTimeMapper.class)
        List<IdCreateTime> insertBatch(String... name);
    }

    public interface RegisterRowMapperDao {
        @SqlUpdate("create table something (id serial, name varchar(50), created_on timestamp default now())")
        void createTable();

        @SqlUpdate("insert into something(name) values (:name)")
        @GetGeneratedKeys({"id", "created_on"})
        @RegisterRowMapper(IdCreateTimeMapper.class)
        IdCreateTime insert(String name);

        @SqlBatch("insert into something(name) values (:name)")
        @GetGeneratedKeys({"id", "created_on"})
        @RegisterRowMapper(IdCreateTimeMapper.class)
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
