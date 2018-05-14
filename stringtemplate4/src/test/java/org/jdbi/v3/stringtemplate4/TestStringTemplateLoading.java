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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.stringtemplate4.TestStringTemplateSqlLocator.Wombat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class TestStringTemplateLoading {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @Before
    public void setUp() throws Exception {
        handle = dbRule.getSharedHandle();
    }

    public void testBaz(int id) {
        Wombat wombat = handle.attach(Wombat.class);
        wombat.insert(new Something(id, "Doo" + id));

        String name = handle.createQuery("select name from something where id = " + id)
                            .mapTo(String.class)
                            .findOnly();

        assertThat(name).isEqualTo("Doo" + id);
    }

    @Test
    public void testConcurrentLoading() throws InterruptedException, ExecutionException {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        IntStream
            .range(1, 10)
            .forEach(id -> pool.execute(() -> testBaz(id)));
        pool.awaitTermination(1000, TimeUnit.MILLISECONDS);
    }
}
