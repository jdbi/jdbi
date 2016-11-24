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
package org.jdbi.v3.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestPreparedBatchGenerateKeysPostgres {

    @Rule
    public PgDatabaseRule pgdb = new PgDatabaseRule().withPreparer(new JdbiPreparer() {
        @Override
        protected void prepare(Handle handle) {
            handle.execute("create table something (id serial, name varchar(50), create_time timestamp default now())");
        }
    });

    private Handle h;

    @Before
    public void getHandle() {
        h = pgdb.getJdbi().open();
    }

    @After
    public void close() {
        h.close();
    }

    @Test
    public void testBatchInsertWithKeyGenerationAndExplicitColumnNames() {
        PreparedBatch batch = h.prepareBatch("insert into something (name) values (?) ");
        batch.add("Brian");
        batch.add("Thom");

        List<Integer> ids = batch.executeAndGenerateKeys(Integer.class, "id").list();
        assertThat(ids).containsExactly(1, 2);

        List<Something> somethings = h.createQuery("select id, name from something")
                .mapToBean(Something.class)
                .list();
        assertThat(somethings).containsExactly(new Something(1, "Brian"), new Something(2, "Thom"));
    }

    @Test
    public void testBatchInsertWithKeyGenerationAndExplicitSeveralColumnNames() {
        PreparedBatch batch = h.prepareBatch("insert into something (name) values (?) ");
        batch.add("Brian");
        batch.add("Thom");

        List<IdCreateTime> ids = batch.executeAndGenerateKeys((r, ctx) -> {
            return new IdCreateTime(r.getInt("id"), r.getDate("create_time"));
        }, "id", "create_time").list();

        assertThat(ids).hasSize(2);
        assertThat(ids).extracting(ic -> ic.id).containsExactly(1, 2);
        assertThat(ids).extracting(ic -> ic.createTime).doesNotContainNull();
    }

    private static class IdCreateTime {

        final Integer id;
        final Date createTime;

        public IdCreateTime(Integer id, Date createTime) {
            this.id = id;
            this.createTime = createTime;
        }
    }
}
