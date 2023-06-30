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
package org.jdbi.v3.generator;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Predicate;

import org.assertj.core.groups.Tuple;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.extension.ExtensionConfigurer;
import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.core.extension.annotation.UseExtensionConfigurer;
import org.jdbi.v3.core.h2.H2DatabasePlugin;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.GenerateSqlObject;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class NonpublicSubclassTest {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2()
        .withPlugins(new H2DatabasePlugin(), new SqlObjectPlugin())
        .withInitializer(TestingInitializers.something())
        .withConfig(Extensions.class, c -> c.setAllowProxy(false));

    private Handle handle;
    private AbstractClassDao dao;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
        handle.registerRowMapper(new SomethingMapper());
        dao = handle.attach(AbstractClassDao.class);
    }

    @Test
    public void simpleGeneratedClass() {
        dao.insert(1, "Bella");
        assertThat(dao.list()).extracting("id", "name")
            .containsExactly(Tuple.tuple(1, "Bella"));
        assertThat(dao.list0(1)).hasSize(1);
        assertThat(dao.list0(0)).isEmpty();

        assertThat(dao.getHandle()).isSameAs(handle);
    }

    @Test
    public void extensionTypeConfigurer() {
        assertThat(dao.checkConfigurer(cfg -> cfg.configuredType)).isTrue();
    }

    @Test
    public void extensionMethodConfigurer() {
        dao.assertConfigurer(cfg -> cfg.configuredMethod);
    }

    @Test
    public void onDemandDefaultMethod() {
        assertThat(h2Extension.getJdbi().onDemand(InterfaceDao.class)
            .defaultMethod()).isEqualTo(2288);
    }

    @Test
    public void abstractClassToString() {
        assertThat(dao.toString())
                .isEqualTo("org.jdbi.v3.generator.AbstractClassDaoImpl@" + Integer.toHexString(System.identityHashCode(dao)));
    }

    @Test
    public void abstractClassEquals() {
        assertThat(dao.equals(dao)).isTrue();
        h2Extension.getJdbi().useExtension(AbstractClassDao.class, anotherDao ->
                assertThat(dao.equals(anotherDao)).isFalse());
    }

    @Test
    public void abstractClassHashCode() {
        assertThat(dao.hashCode())
                .isEqualTo(System.identityHashCode(dao));
    }

    @Test
    public void interfaceToString() {
        InterfaceDao myDao = handle.getJdbi().onDemand(InterfaceDao.class);
        assertThat(myDao.toString())
                .isEqualTo("org.jdbi.v3.generator.InterfaceDaoImpl$OnDemand@" + Integer.toHexString(System.identityHashCode(myDao)));
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void interfaceClassEquals() {
        InterfaceDao ifDao = handle.getJdbi().onDemand(InterfaceDao.class);
        assertThat(ifDao.equals(ifDao)).isTrue();
        assertThat(ifDao.equals(dao)).isFalse();
    }

    @Test
    public void interfaceHashCode() {
        InterfaceDao myDao = handle.getJdbi().onDemand(InterfaceDao.class);
        assertThat(myDao.hashCode())
                .isEqualTo(System.identityHashCode(myDao));
    }

    @GenerateSqlObject
    @GeneratorConfigurerType
    abstract static class AbstractClassDao implements SqlObject {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        abstract void insert(int id, String name);

        @GeneratorConfigurerMethod
        public boolean checkConfigurer(Predicate<GeneratorConfig> test) {
            final GeneratorConfig cfg = getHandle().getConfig(GeneratorConfig.class);
            return test.test(cfg);
        }

        // test both value-returning and void methods
        public void assertConfigurer(Predicate<GeneratorConfig> test) {
            assertThat(checkConfigurer(test)).isTrue();
        }

        @SqlQuery("select * from something")
        abstract List<Something> list0();
        @SqlQuery("select * from something where id = :id")
        abstract List<Something> list0(int id);

        public List<Something> list() {
            if (password() != 42) {
                throw new AssertionError();
            }
            return list0();
        }

        private int password() {
            return 42;
        }
    }

    @GenerateSqlObject
    interface InterfaceDao extends SqlObject {
        default int defaultMethod() {
            return 2288;
        }
    }

    @UseExtensionConfigurer(GeneratorConfigurerTypeImpl.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface GeneratorConfigurerType {}

    @UseExtensionConfigurer(GeneratorConfigurerMethodImpl.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface GeneratorConfigurerMethod {}

    public static class GeneratorConfigurerTypeImpl implements ExtensionConfigurer {
        @Override
        public void configureForType(ConfigRegistry config, Annotation annotation, Class<?> sqlObjectType) {
            config.get(GeneratorConfig.class).configuredType = true;
        }
    }

    public static class GeneratorConfigurerMethodImpl implements ExtensionConfigurer {
        @Override
        public void configureForMethod(ConfigRegistry config, Annotation annotation, Class<?> sqlObjectType, Method method) {
            config.get(GeneratorConfig.class).configuredMethod = true;
        }
    }

    public static class GeneratorConfig implements JdbiConfig<GeneratorConfig> {
        boolean configuredType = false;
        boolean configuredMethod = false;

        public GeneratorConfig() {}

        public GeneratorConfig(GeneratorConfig other) {
            configuredMethod = other.configuredMethod;
            configuredType = other.configuredType;
        }

        @Override
        public GeneratorConfig createCopy() {
            return new GeneratorConfig(this);
        }
    }
}
