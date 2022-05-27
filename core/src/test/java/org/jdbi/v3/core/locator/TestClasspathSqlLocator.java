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

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.statement.StatementException;
import org.jdbi.v3.core.statement.StatementExceptions;
import org.jdbi.v3.core.statement.StatementExceptions.MessageRendering;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestClasspathSqlLocator {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.withSomething();

    @Test
    public void testLocateNamed() {
        Handle h = h2Extension.openHandle();
        h.execute(ClasspathSqlLocator.findSqlOnClasspath("insert-keith"));
        assertThat(h.select("select name from something").mapTo(String.class).list()).hasSize(1);
    }

    @Test
    public void testCommentsInExternalSql() {
        Handle h = h2Extension.openHandle();
        h.execute(ClasspathSqlLocator.findSqlOnClasspath("insert-eric-with-comments"));
        assertThat(h.select("select name from something").mapTo(String.class).list()).hasSize(1);
    }

    @Test
    public void testPositionalParamsInPrepared() {
        Handle h = h2Extension.openHandle();
        h.execute(ClasspathSqlLocator.findSqlOnClasspath("insert-id-name-positional"), 3, "Tip");
        assertThat(h.select("select name from something").mapTo(String.class).list()).hasSize(1);
    }

    @Test
    public void testNamedParamsInExternal() {
        Handle h = h2Extension.openHandle();
        h.createUpdate(ClasspathSqlLocator.findSqlOnClasspath("insert-id-name"))
                .bind("id", 1)
                .bind("name", "Tip")
                .execute();
        assertThat(h.select("select name from something").mapTo(String.class).list()).hasSize(1);
    }

    @Test
    public void testUsefulExceptionForBackTracing() {
        Handle h = h2Extension.openHandle();

        assertThatThrownBy(() -> h.createUpdate(ClasspathSqlLocator.findSqlOnClasspath("insert-id-name"))
                .bind("id", 1)
                .execute())
            .isInstanceOf(StatementException.class)
            .hasMessageContaining("Missing named parameter 'name'")
            .hasMessageContaining("id:1");
    }

    @Test
    public void testDetailExceptionForBackTracing() {
        Handle h = h2Extension.openHandle();
        h.getConfig(StatementExceptions.class).setMessageRendering(MessageRendering.DETAIL);

        assertThatThrownBy(() -> h.createUpdate(ClasspathSqlLocator.findSqlOnClasspath("insert-id-name"))
                .bind("id", 1)
                .execute())
            .isInstanceOf(StatementException.class)
            .hasMessageContaining("insert into something(id, name) values (:id, :name)")
            .hasMessageContaining("insert into something(id, name) values (?, ?)")
            .hasMessageContaining("id:1");
    }

    @Test
    public void testNonExistentResource() {
        assertThatThrownBy(() -> ClasspathSqlLocator.findSqlOnClasspath("this-does-not-exist"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testCachesResultAfterFirstLookup() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final AtomicInteger loadCount = new AtomicInteger(0);

        Thread.currentThread().setContextClassLoader(new ClassLoader(classLoader) {
            @Override
            public InputStream getResourceAsStream(String s) {
                loadCount.incrementAndGet();
                return super.getResourceAsStream(s);
            }
        });

        ClasspathSqlLocator.findSqlOnClasspath("caches-result-after-first-lookup");
        assertThat(loadCount.get()).isEqualTo(1);

        ClasspathSqlLocator.findSqlOnClasspath("caches-result-after-first-lookup");
        assertThat(loadCount.get()).isEqualTo(1); // has not increased since previous

        Thread.currentThread().setContextClassLoader(classLoader);
    }

    @Test
    public void testLocateByMethodName() {
        assertThat(ClasspathSqlLocator.findSqlOnClasspath(getClass(), "testLocateByMethodName"))
                .contains("select 1");
    }

    @Test
    public void testSelectByExtensionMethodName() {
        assertThat(ClasspathSqlLocator.findSqlOnClasspath(getClass(), "test-locate-by-custom-name"))
                .contains("select 1");
    }

    @Test
    public void testColonInComment() {
        // Used to throw exception in SQL statement lexer
        // see https://github.com/jdbi/jdbi/issues/748
        assertThat(ClasspathSqlLocator.findSqlOnClasspath(getClass(), "test-colon-in-comment"))
            .contains("SELECT 1.007 AS column_name");
    }
}
