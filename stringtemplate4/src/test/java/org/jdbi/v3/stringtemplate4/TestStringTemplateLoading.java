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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.internal.exceptions.Unchecked;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.stringtemplate4.TestStringTemplateSqlLocator.Wombat;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestStringTemplateLoading {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2()
        .enableLeakChecker(false) // otherwise the test may run out of memory
        .withInitializer(TestingInitializers.something())
        .withPlugin(new SqlObjectPlugin());

    private Handle singleThreadedHandle;
    private Wombat shareableDao;
    private ExecutorService pool;
    private Jdbi jdbi;
    private final AtomicInteger oid = new AtomicInteger();

    @BeforeEach
    public void setUp() {
        pool = Executors.newFixedThreadPool(10);
        jdbi = h2Extension.getJdbi();

        singleThreadedHandle = h2Extension.getSharedHandle();
        shareableDao = h2Extension.getJdbi().onDemand(Wombat.class);
    }

    @AfterEach
    public void tearDown() throws Exception {
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    public void testSlowConcurrentLoading() {
        doLoad(this::slowLoader);
    }

    @Test
    public void testFastConcurrentLoading() {
        doLoad(this::fastLoader);
    }

    private void doLoad(Runnable runnable) {
        for (int i = 0; i < 20; i++) {
            List<Future<?>> futures = IntStream
                .range(1, 10)
                .mapToObj(id -> pool.submit(runnable))
                .collect(Collectors.toList());
            futures.forEach(Unchecked.consumer(f -> f.get(5, TimeUnit.SECONDS)));
        }
    }

    // The loader shares the handle across multiple threads. However a handle is
    // a single-threaded object so any access must be synchronized to ensure that only
    // one thread at a time accesses the handle.
    private synchronized void slowLoader() {
        int id = oid.getAndIncrement();

        Wombat wombat = singleThreadedHandle.attach(Wombat.class);
        wombat.insert(new Something(id, "Doo" + id));

        String name = singleThreadedHandle.createQuery("select name from something where id = " + id)
            .mapTo(String.class)
            .one();

        assertThat(name).isEqualTo("Doo" + id);
    }

    public void fastLoader() {
        int id = oid.getAndIncrement();

        // this is a global object and executing an operation on it
        // creates a new handle behind the scenes. So it can be shared between threads.
        shareableDao.insert(new Something(id, "Doo" + id));

        jdbi.useHandle(handle -> {
            String name = handle.createQuery("select name from something where id = " + id)
                .mapTo(String.class)
                .one();

            assertThat(name).isEqualTo("Doo" + id);
        });
    }
}
