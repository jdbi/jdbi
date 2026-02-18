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
package org.jdbi.core.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.jdbi.core.Jdbi;
import org.jdbi.core.cache.internal.DefaultJdbiCacheBuilder;
import org.jdbi.core.internal.testing.H2DatabaseExtension;
import org.jdbi.core.statement.Query;
import org.jdbi.core.statement.SqlStatements;
import org.jdbi.core.statement.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static java.lang.String.format;

import static org.assertj.core.api.Assertions.assertThat;

class DeadlockTest {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(H2DatabaseExtension.SOMETHING_INITIALIZER);

    static final int THREAD_COUNT = 100;

    Jdbi jdbi;

    @BeforeEach
    void before() {
        jdbi = h2Extension.getJdbi();

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int id = i;
            jdbi.useHandle(h -> {
                try (Update u = h.createUpdate("INSERT INTO something (id, name) VALUES (:id, :name)")) {
                    u.bind("id", id)
                            .bind("name", "name_" + id)
                            .execute();
                }
            });
        }
    }

    @Test
    void testIssue2274() throws Exception {
        jdbi.getConfig(SqlStatements.class).setTemplateCache(DefaultJdbiCacheBuilder.builder().maxSize(10));
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT,
                new ThreadFactoryBuilder().setNameFormat("test-%d").setDaemon(true).build());

        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int id = i;
            Callable<Integer> c = () -> {
                return jdbi.withHandle(h -> {
                    try (Query q = h.createQuery(format("SELECT <value> FROM something where %d = :id AND id = :id", id))) {
                        q.bind("id", id);
                        q.define("value", id);
                        return q.mapTo(Integer.class).one();
                    }
                });
            };
            futures.add(executorService.submit(c));
        }

        for (int i = 0; i < THREAD_COUNT; i++) {
            assertThat(futures.get(i).get()).isEqualTo(i);
        }
    }
}
