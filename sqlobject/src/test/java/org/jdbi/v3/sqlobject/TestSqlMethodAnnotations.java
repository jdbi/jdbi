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
package org.jdbi.v3.sqlobject;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSqlMethodAnnotations
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        handle = db.getSharedHandle();
    }

    @Test(expected = IllegalStateException.class)
    public void testMutuallyExclusiveAnnotations()
    {
        handle.attach(Broken.class);
    }

    public interface Broken {
        @SqlQuery
        @SqlUpdate
        void bogus();
    }

    @Test
    public void testCustomAnnotation() {
        Dao dao = handle.attach(Dao.class);

        assertThat(dao.foo()).isEqualTo("foo");
    }

    public interface Dao {
        @Foo
        String foo();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @SqlMethodAnnotation(Foo.Factory.class)
    public @interface Foo {
        class Factory implements HandlerFactory {
            @Override
            public Handler buildHandler(Class<?> sqlObjectType, Method method) {
                return (obj, m, args, handle) -> "foo";
            }
        }
    }
}
