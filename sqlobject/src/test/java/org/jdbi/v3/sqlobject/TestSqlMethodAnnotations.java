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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestSqlMethodAnnotations {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
    }

    @Test
    public void testMutuallyExclusiveAnnotations() {
        assertThatThrownBy(() -> handle.attach(Broken.class)).isInstanceOf(IllegalStateException.class);
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
    @SqlOperation(Foo.Impl.class)
    public @interface Foo {
        class Impl implements Handler {
            @Override
            public Object invoke(Object target, Object[] args, HandleSupplier h) {
                return "foo";
            }
        }
    }
}
