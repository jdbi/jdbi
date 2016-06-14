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
package org.jdbi.v3.locator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.locator.ClasspathSqlLocator.findSqlOnClasspath;
import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.Handle;
import org.jdbi.v3.exception.StatementException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestClasspathSqlLocator {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
    }

    @Test
    public void testLocateNamed() throws Exception {
        Handle h = db.openHandle();
        h.insert(findSqlOnClasspath("insert-keith"));
        assertEquals(1, h.select("select name from something").size());
    }

    @Test
    public void testCommentsInExternalSql() throws Exception {
        Handle h = db.openHandle();
        h.insert(findSqlOnClasspath("insert-eric-with-comments"));
        assertEquals(1, h.select("select name from something").size());
    }

    @Test
    public void testNamedPositionalNamedParamsInPrepared() throws Exception {
        Handle h = db.openHandle();
        h.insert(findSqlOnClasspath("insert-id-name"), 3, "Tip");
        assertEquals(1, h.select("select name from something").size());
    }

    @Test
    public void testNamedParamsInExternal() throws Exception {
        Handle h = db.openHandle();
        h.createStatement(findSqlOnClasspath("insert-id-name"))
                .bind("id", 1)
                .bind("name", "Tip")
                .execute();
        assertEquals(1, h.select("select name from something").size());
    }

    @Test
    public void testUsefulExceptionForBackTracing() throws Exception {
        Handle h = db.openHandle();

        exception.expect(StatementException.class);
        exception.expectMessage("insert into something(id, name) values (:id, :name)");
        exception.expectMessage("insert into something(id, name) values (?, ?)");
        h.createStatement(findSqlOnClasspath("insert-id-name"))
                .bind("id", 1)
                .execute();
    }

    @Test
    public void testNonExistentResource() throws Exception {
        exception.expect(IllegalArgumentException.class);
        findSqlOnClasspath("this-does-not-exist");
    }

    @Test
    public void testCachesResultAfterFirstLookup() throws Exception {
        ClassLoader ctx_loader = Thread.currentThread().getContextClassLoader();
        final AtomicInteger load_count = new AtomicInteger(0);
        Thread.currentThread().setContextClassLoader(new ClassLoader(ctx_loader) {
            @Override
            public InputStream getResourceAsStream(String s) {
                // will be called twice, once for raw name, once for name + .sql
                InputStream in = super.getResourceAsStream(s);
                load_count.incrementAndGet();
                return in;
            }
        });

        findSqlOnClasspath("caches-result-after-first-lookup");
        assertThat(load_count.get()).isEqualTo(1);

        findSqlOnClasspath("caches-result-after-first-lookup");
        assertThat(load_count.get()).isEqualTo(1); // has not increased since previous

        Thread.currentThread().setContextClassLoader(ctx_loader);
    }

    @Test
    public void testLocateByMethodName() throws Exception {
        assertThat(findSqlOnClasspath(getClass(), "testLocateByMethodName"))
                .contains("select 1");
    }

    @Test
    public void testSelectByExtensionMethodName() throws Exception {
        assertThat(findSqlOnClasspath(getClass(), "test-locate-by-custom-name"))
                .contains("select 1");
    }
}
