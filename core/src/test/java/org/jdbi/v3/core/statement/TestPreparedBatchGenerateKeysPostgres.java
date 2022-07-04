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
package org.jdbi.v3.core.statement;

import java.util.Date;
import java.util.List;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.junit5.PgDatabaseExtension;
import org.jdbi.v3.core.result.ResultBearingAndModCounts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestPreparedBatchGenerateKeysPostgres {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public PgDatabaseExtension pgExtension = PgDatabaseExtension.instance(pg).withInitializer(
        handle -> handle.execute("create table something (id serial, name varchar(50), create_time timestamp default now())")
    );

    @Test
    public void testBatchInsertWithKeyGenerationAndExplicitColumnNames() {
        Handle h = pgExtension.openHandle();

        PreparedBatch batch = h.prepareBatch("insert into something (name) values (?) ");
        batch.add("Brian");
        batch.add("Thom");

        List<Integer> ids = batch.executeAndReturnGeneratedKeys("id").mapTo(Integer.class).list();
        assertThat(ids).containsExactly(1, 2);

        List<Something> somethings = h.createQuery("select id, name from something")
            .mapToBean(Something.class)
            .list();
        assertThat(somethings).containsExactly(new Something(1, "Brian"), new Something(2, "Thom"));
    }

    @Test
    public void testBatchInsertWithKeyGenerationAndExplicitSeveralColumnNames() {
        Handle h = pgExtension.openHandle();

        PreparedBatch batch = h.prepareBatch("insert into something (name) values (?) ");
        batch.add("Brian");
        batch.add("Thom");

        List<IdCreateTime> ids = batch.executeAndReturnGeneratedKeys("id", "create_time")
            .map((r, ctx) -> new IdCreateTime(r.getInt("id"), r.getDate("create_time")))
            .list();

        assertThat(ids).hasSize(2);
        assertThat(ids).extracting(ic -> ic.id).containsExactly(1, 2);
        assertThat(ids).extracting(ic -> ic.createTime).doesNotContainNull();
    }

    @Test
    public void testBatchUpdateWithResultBearingAndModCount() {
        Handle h = pgExtension.openHandle();

        PreparedBatch batch1 = h.prepareBatch("insert into something (name) values (?) ");
        batch1.add("Brian1");
        batch1.add("Brian2");
        batch1.add("Thom1");
        batch1.add("Thom2");
        List<IdCreateTime> ids = batch1.executeAndReturnGeneratedKeys("id", "create_time")
            .map((r, ctx) -> new IdCreateTime(r.getInt("id"), r.getTimestamp("create_time")))
            .list();

        PreparedBatch batch2 = h.prepareBatch("update something set create_time = now() where name like :name returning id, name, create_time");
        batch2
            .bind("name", "Brian%")
            .add()
            .bind("name", "Thom%")
            .add()
            .bind("name", "Nothing%")
            .add();

        ResultBearingAndModCounts resultBearingAndModCounts = batch2.executeAndReturnGeneratedKeysAndModCounts("id", "create_time");
        List<List<IdCreateTime>> choppedList = resultBearingAndModCounts
            .resultBearing()
            .map((r, ctx) -> new IdCreateTime(r.getInt("id"), r.getTimestamp("create_time")))
            .lists(resultBearingAndModCounts);

        assertThat(choppedList).hasSize(3);
        assertThat(choppedList.get(0)).extracting(ic -> ic.id).containsExactly(1, 2);
        assertThat(choppedList.get(0)).extracting(ic -> ic.createTime).allMatch(date -> ids.stream().map(idCreateTime -> idCreateTime.createTime).allMatch(date1 -> date1.before(date)));
        assertThat(choppedList.get(1)).extracting(ic -> ic.id).containsExactly(3, 4);
        assertThat(choppedList.get(1)).extracting(ic -> ic.createTime).allMatch(date -> ids.stream().map(idCreateTime -> idCreateTime.createTime).allMatch(date1 -> date1.before(date)));
        assertThat(choppedList.get(2)).extracting(ic -> ic.id).isEmpty();
        assertThat(choppedList.get(2)).extracting(ic -> ic.createTime).isEmpty();
    }

    private static class IdCreateTime {

        final Integer id;
        final Date createTime;

        IdCreateTime(Integer id, Date createTime) {
            this.id = id;
            this.createTime = createTime;
        }
    }
}
