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
package org.skife.jdbi.v2;

import org.junit.Test;
import org.skife.jdbi.v2.exceptions.StatementException;
import org.skife.jdbi.v2.exceptions.UnableToCreateStatementException;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.cglib.transform.AbstractClassLoader;
import net.sf.cglib.transform.ClassFilter;
import org.skife.jdbi.v2.sqlobject.stringtemplate.TestingStatementContext;
import org.skife.jdbi.v2.tweak.StatementLocator;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 */
public class TestClasspathStatementLocator extends DBITestCase
{
    @Test
    public void testLocateNamedWithoutSuffix() throws Exception {
        Handle h = openHandle();
        h.createStatement("insert-keith").execute();
        assertEquals(1, h.select("select name from something").size());
    }

    @Test
    public void testLocateNamedWithSuffix() throws Exception {
        Handle h = openHandle();
        h.insert("insert-keith.sql");
        assertEquals(1, h.select("select name from something").size());
    }

    @Test
    public void testCommentsInExternalSql() throws Exception {
        Handle h = openHandle();
        h.insert("insert-eric-with-comments");
        assertEquals(1, h.select("select name from something").size());
    }

    @Test
    public void testNamedPositionalNamedParamsInPrepared() throws Exception {
        Handle h = openHandle();
        h.insert("insert-id-name", 3, "Tip");
        assertEquals(1, h.select("select name from something").size());
    }

    @Test
    public void testNamedParamsInExternal() throws Exception {
        Handle h = openHandle();
        h.createStatement("insert-id-name").bind("id", 1).bind("name", "Tip").execute();
        assertEquals(1, h.select("select name from something").size());
    }

    @Test
    public void testUsefulExceptionForBackTracing() throws Exception {
        Handle h = openHandle();

        try {
            h.createStatement("insert-id-name").bind("id", 1).execute();
            fail("should have raised an exception");
        }
        catch (StatementException e) {
            assertTrue(e.getMessage().contains("insert into something(id, name) values (:id, :name)"));
            assertTrue(e.getMessage().contains("insert into something(id, name) values (?, ?)"));
            assertTrue(e.getMessage().contains("insert-id-name"));
        }

    }

    @Test
    public void testTriesToParseNameIfNothingFound() throws Exception {
        Handle h = openHandle();
        try {
            h.insert("this-does-not-exist.sql");
            fail("Should have raised an exception");
        }
        catch (UnableToCreateStatementException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testCachesResultAfterFirstLookup() throws Exception
    {
        ClassLoader ctx_loader = Thread.currentThread().getContextClassLoader();
        final AtomicInteger load_count = new AtomicInteger(0);
        Thread.currentThread().setContextClassLoader(new AbstractClassLoader(ctx_loader, ctx_loader, new NoClassFilter())
        {
            @Override
            public InputStream getResourceAsStream(String s)
            {
                // will be called twice, once for raw name, once for name + .sql
                InputStream in = super.getResourceAsStream(s);
                load_count.incrementAndGet();
                return in;
            }
        });

        Handle h = openHandle();
        h.execute("caches-result-after-first-lookup", 1, "Brian");
        assertThat(load_count.get(), equalTo(2)); // two lookups, name and name.sql

        h.execute("caches-result-after-first-lookup", 2, "Sean");
        assertThat(load_count.get(), equalTo(2)); // has not increased since previous

        Thread.currentThread().setContextClassLoader(ctx_loader);

    }

    @Test
    public void testCachesOriginalQueryWhenNotFound() throws Exception
    {
        StatementLocator statementLocator = new ClasspathStatementLocator();
        StatementContext statementContext = new TestingStatementContext(new HashMap<String, Object>()) {

            @Override
            public Class<?> getSqlObjectType() {
                return TestClasspathStatementLocator.class;
            }

            @Override
            public Method getSqlObjectMethod() {
                return null;
            }
        };

        String input = "missing query";
        String located = statementLocator.locate(input, statementContext);

        assertEquals(input, located); // first time just caches it

        located = statementLocator.locate(input, statementContext);

        assertEquals(input, located); // second time reads from cache
    }

    @Test
    public void testCachesOriginalQueryByMethodWhenNotFound() throws Exception
    {
        StatementLocator statementLocator = new ClasspathStatementLocator();
        StatementContext statementContext = new TestingStatementContext(new HashMap<String, Object>()) {

            @Override
            public Class<?> getSqlObjectType() {
                return TestClasspathStatementLocator.class;
            }

            @Override
            public Method getSqlObjectMethod() {
                try {
                    return TestClasspathStatementLocator.class.getMethod("testCachesOriginalQueryByMethodWhenNotFound");
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        String input = "missing query";
        String located = statementLocator.locate(input, statementContext);

        assertEquals(input, located); // first time just caches it

        located = statementLocator.locate(input, statementContext);

        assertEquals(input, located); // second time reads from cache
    }

    static class NoClassFilter implements ClassFilter
    {
        @Override
        public boolean accept(String className) {
            return false;
        }
    }
}
