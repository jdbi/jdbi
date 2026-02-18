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

import java.io.IOException;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.core.Something;
import org.jdbi.core.mapper.SomethingMapper;
import org.jdbi.core.transaction.TransactionIsolationLevel;
import org.jdbi.sqlobject.config.RegisterRowMapper;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.sqlobject.transaction.Transaction;
import org.jdbi.sqlobject.transaction.Transactional;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestPostgresBugs {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg)
        .withPlugin(new SqlObjectPlugin())
        .withInitializer((ds, handle) -> {
            handle.execute("create table if not exists something (id int primary key, name varchar(100))");
            handle.execute("delete from something");
        });

    @Test
    public void testConnected() {
        int four = pgExtension.getJdbi().withHandle(handle ->
            handle.createQuery("select 2 + 2").mapTo(Integer.class).one());

        assertThat(four).isEqualTo(4);
    }

    @Test
    public void testTransactions() {
        Dao dao = pgExtension.getJdbi().onDemand(Dao.class);

        Something s = dao.insertAndFetch(1, "Brian");
        assertThat(s).isEqualTo(new Something(1, "Brian"));
    }

    @Test
    public void testExplicitTransaction() {
        Dao dao = pgExtension.getJdbi().onDemand(Dao.class);

        Something s = dao.inTransaction(transactional -> {
            transactional.insert(1, "Brian");
            return transactional.findById(1);
        });

        assertThat(s).isEqualTo(new Something(1, "Brian"));
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface Dao extends Transactional<Dao> {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select id, name from something where id = :id")
        Something findById(@Bind("id") int id);

        @Transaction(TransactionIsolationLevel.READ_COMMITTED)
        default Something insertAndFetch(int id, String name) {
            insert(id, name);
            return findById(id);
        }

        @Transaction
        default Something failed(int id, String name) throws IOException {
            insert(id, name);
            throw new IOException("woof");
        }
    }
}
