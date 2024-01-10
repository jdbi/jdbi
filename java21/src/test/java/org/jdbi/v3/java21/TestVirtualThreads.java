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
package org.jdbi.v3.java21;

import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class TestVirtualThreads {

    @RegisterExtension
    JdbiExtension h2Extension = JdbiExtension.h2()
            .withPlugin(new SqlObjectPlugin())
            .withInitializer(TestingInitializers.usersWithData());

    @Test
    void virtualThreads() {
        final var dao = h2Extension.getJdbi().onDemand(UserDao.class);
        final var inserts = IntStream.range(100, 200)
                .mapToObj(id -> new User(id, "User " + id))
                .toList();
        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            inserts.forEach(insert ->
                    exec.submit(() -> {
                        dao.useTransaction(txn -> {
                            txn.insertUser(insert);
                            try {
                                Thread.sleep((long) (Math.random() * 100));
                            } catch (final InterruptedException e) {
                                throw new AssertionError(e);
                            }
                        });
                    }));
        }
        assertThat(dao.countUsers()).isEqualTo(102);
    }

    @Test
    void virtualThreadsAndLimitedPool() {
        final var hikariCfg = new HikariConfig();
        hikariCfg.setMaximumPoolSize(10);
        hikariCfg.setJdbcUrl(h2Extension.getUrl());
        try (final var pool = new HikariDataSource(hikariCfg)) {
            final var dao = Jdbi.create(pool).installPlugin(new SqlObjectPlugin()).onDemand(UserDao.class);
            final var inserts = IntStream.range(100, 200)
                    .mapToObj(id -> new User(id, "User " + id))
                    .toList();
            try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
                inserts.forEach(insert ->
                        exec.submit(() -> {
                            dao.useTransaction(txn -> {
                                txn.insertUser(insert);
                                try {
                                    Thread.sleep((long) (Math.random() * 100));
                                } catch (final InterruptedException e) {
                                    throw new AssertionError(e);
                                }
                            });
                        }));
            }
            assertThat(dao.countUsers()).isEqualTo(102);
        }
    }

    public record User(int id, String name) {}

    interface UserDao extends Transactional<UserDao> {

        @SqlQuery("""
                SELECT count(1) FROM users
                """)
        int countUsers();

        @SqlUpdate("""
                INSERT INTO users (id, name)
                VALUES (:id, :name)
                """)
        void insertUser(@BindMethods User user);
    }
}
