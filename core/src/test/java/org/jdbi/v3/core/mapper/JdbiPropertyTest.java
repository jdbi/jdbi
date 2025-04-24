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
package org.jdbi.v3.core.mapper;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.immutables.value.Value;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.annotation.JdbiProperty;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.immutables.JdbiImmutables;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.mapper.reflect.FieldMapper;
import org.jdbi.v3.core.statement.BindingAccess;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementCustomizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class JdbiPropertyTest {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    protected Handle handle;

    @BeforeEach
    void setUp() {
        this.handle = h2Extension.getSharedHandle();
    }

    @Test
    public void beanIgnoreMap() {
        assertThat(handle.select("select 1 as id, 2 as unmapped, 3 as unbound")
                        .map(BeanMapper.of(TestBean.class))
                        .one())
                .extracting(TestBean::getId, TestBean::getUnmapped, TestBean::getUnbound)
                .containsExactly(1, 0, 3);
    }

    @Test
    public void beanIgnoreBind() {
        assertThat(handle.select("select :id * 10 + :unmapped")
                .bindBean(new TestBean(2, 3, 4))
                .addCustomizer(new UnboundDetector())
                .mapTo(int.class))
            .containsExactly(23);
    }

    public static class TestBean {

        private int id = 0;
        private int unmapped = 0;
        private int unbound = 0;

        public TestBean() {}

        public TestBean(int id, int unmapped, int unbound) {
            this.id = id;
            this.unmapped = unmapped;
            this.unbound = unbound;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getUnmapped() {
            return unmapped;
        }

        @JdbiProperty(map = false)
        public void setUnmapped(int unmapped) {
            this.unmapped = unmapped;
        }

        @JdbiProperty(bind = false)
        public int getUnbound() {
            return unbound;
        }

        public void setUnbound(int unbound) {
            this.unbound = unbound;
        }
    }

    @Test
    public void fieldsIgnoreMap() {
        assertThat(handle.select("select 1 as id, 2 as unmapped, 3 as unbound")
                .map(FieldMapper.of(TestFields.class)).one())
            .extracting(tf -> tf.id, tf -> tf.unmapped, tf -> tf.unbound)
            .containsExactly(1, 0, 3);
    }

    @Test
    public void fieldIgnoreBind() {
        final TestFields tf = new TestFields();
        tf.id = 2;
        tf.unmapped = 3;
        tf.unbound = 4;
        assertThat(handle.select("select :id * 10 + :unmapped")
                .bindFields(tf)
                .addCustomizer(new UnboundDetector())
                .mapTo(int.class))
            .containsExactly(23);
    }

    public static class TestFields {
        public int id = 0;

        @JdbiProperty(map = false)
        public int unmapped = 0;

        @JdbiProperty(bind = false)
        public int unbound = 0;
    }

    @Test
    public void methodsIgnoreBind() {
        assertThat(handle.select("select :id * 10 + :unmapped")
                .bindMethods(new TestMethods())
                .addCustomizer(new UnboundDetector())
                .mapTo(int.class))
            .containsExactly(23);
    }

    public static class TestMethods {
        public int id() {
            return 2;
        }

        @JdbiProperty(map = false)
        public int unmapped() {
            return 3;
        }

        @JdbiProperty(bind = false)
        public int unbound() {
            return 4;
        }
    }

    @Test
    public void immutablesBindDefineIgnore() {
        handle.getConfig(JdbiImmutables.class).registerImmutable(TestImmutables.class);
        assertThat(handle.select("select :id")
                .bindPojo(ImmutableTestImmutables.builder().build())
                .defineNamedBindings()
                .addCustomizer(new UnboundDetector())
                .addCustomizer(new UndefinedDetector())
                .mapTo(int.class))
            .containsExactly(42);
    }

    @Value.Immutable
    public interface TestImmutables {
        @Value.Default
        default int id() {
            return 42;
        }

        @JdbiProperty(map = false)
        @Value.Default
        default int unmapped() {
            return 3;
        }

        @JdbiProperty(bind = false)
        @Value.Default
        default int unbound() {
            return 4;
        }

        @Value.Check
        @JdbiProperty(bind = false, map = false)
        default TestImmutables doSomeNormalization() {
            return this;
        }
    }

    static final class UnboundDetector implements StatementCustomizer {
        @Override
        public void beforeExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException {
            assertThat(BindingAccess.findForName(ctx.getBinding(), "unbound")).isEmpty();
            assertThat(BindingAccess.getNames(ctx.getBinding())).doesNotContain("unbound");
        }
    }

    static final class UndefinedDetector implements StatementCustomizer {
        @Override
        public void beforeExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException {
            assertThat(ctx.getAttributes()).doesNotContainKey("unbound");
        }
    }
}
