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
package org.jdbi.v3.core.locator;

import static org.jdbi.v3.core.locator.ClasspathSqlLocator.findSqlOnClasspath;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.StatementException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestClasspathSqlLocator {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testLocateNamed() throws Exception {
        Handle h = dbRule.openHandle();
        h.execute(findSqlOnClasspath("insert-keith"));
        assertThat(h.select("select name from something").mapTo(String.class).list()).hasSize(1);
    }

    @Test
    public void testCommentsInExternalSql() throws Exception {
        Handle h = dbRule.openHandle();
        h.execute(findSqlOnClasspath("insert-eric-with-comments"));
        assertThat(h.select("select name from something").mapTo(String.class).list()).hasSize(1);
    }

    @Test
    public void testPositionalParamsInPrepared() throws Exception {
        Handle h = dbRule.openHandle();
        h.execute(findSqlOnClasspath("insert-id-name-positional"), 3, "Tip");
        assertThat(h.select("select name from something").mapTo(String.class).list()).hasSize(1);
    }

    @Test
    public void testNamedParamsInExternal() throws Exception {
        Handle h = dbRule.openHandle();
        h.createUpdate(findSqlOnClasspath("insert-id-name"))
                .bind("id", 1)
                .bind("name", "Tip")
                .execute();
        assertThat(h.select("select name from something").mapTo(String.class).list()).hasSize(1);
    }

    @Test
    public void testUsefulExceptionForBackTracing() throws Exception {
        Handle h = dbRule.openHandle();

        exception.expect(StatementException.class);
        exception.expectMessage("insert into something(id, name) values (:id, :name)");
        exception.expectMessage("insert into something(id, name) values (?, ?)");
        h.createUpdate(findSqlOnClasspath("insert-id-name"))
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
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final AtomicInteger loadCount = new AtomicInteger(0);

        Thread.currentThread().setContextClassLoader(new ClassLoader(classLoader) {
            @Override
            public InputStream getResourceAsStream(String s) {
                loadCount.incrementAndGet();
                return super.getResourceAsStream(s);
            }
        });

        findSqlOnClasspath("caches-result-after-first-lookup");
        assertThat(loadCount.get()).isEqualTo(1);

        findSqlOnClasspath("caches-result-after-first-lookup");
        assertThat(loadCount.get()).isEqualTo(1); // has not increased since previous

        Thread.currentThread().setContextClassLoader(classLoader);
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

    @Test
    public void testColonInComment() throws Exception {
        // Used to throw exception in SQL statement lexer
        // see https://github.com/jdbi/jdbi/issues/748
        findSqlOnClasspath(getClass(), "test-colon-in-comment");
    }
}
