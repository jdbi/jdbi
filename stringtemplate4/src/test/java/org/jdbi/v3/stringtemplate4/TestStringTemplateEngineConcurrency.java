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
package org.jdbi.v3.stringtemplate4;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders one cached template from many threads at once, each with different per-call attributes, to confirm
 * the pooled compiled prototypes are never used by two threads at once and every render is correct.
 */
public class TestStringTemplateEngineConcurrency {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2();

    private static final String SQL =
            "select count(*) from tbl where id = :id <if(byName)> and name = :nm <endif>";

    @Test
    public void concurrentRendersOfOneTemplateAreIsolated() throws Exception {
        Jdbi jdbi = h2Extension.getJdbi();
        jdbi.setTemplateEngine(new StringTemplateEngine());
        jdbi.useHandle(h -> {
            h.execute("create table tbl (id int, name varchar)");
            h.execute("insert into tbl (id, name) values (1, 'a')");
        });

        int threads = 8;
        int iterations = 2000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger wrong = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            final int seed = t;
            futures.add(pool.submit(() -> {
                for (int i = 0; i < iterations; i++) {
                    // Vary the rendered shape and the bound name per call.
                    boolean byName = ((seed + i) & 1) == 0;
                    String name = ((seed + i) & 2) == 0 ? "a" : "z";
                    long expected = !byName ? 1 : ("a".equals(name) ? 1 : 0);
                    long count = jdbi.withHandle(h -> h.createQuery(SQL)
                            .define("byName", byName)
                            .bind("id", 1)
                            .bind("nm", name)
                            .mapTo(Long.class)
                            .one());
                    if (count != expected) {
                        wrong.incrementAndGet();
                    }
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(wrong).hasValue(0);
    }
}
