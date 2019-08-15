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

import com.google.common.base.Predicate;
import org.assertj.core.groups.Tuple;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.GenerateSqlObject;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.config.Configurer;
import org.jdbi.v3.sqlobject.config.ConfiguringAnnotation;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NonpublicSubclassTest {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin()).withSomething()
        .withConfig(Extensions.class, c -> c.setAllowProxy(false));

    private Handle handle;
    private AbstractClassDao dao;

    @Before
    public void setUp() {
        handle = dbRule.getSharedHandle();
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
        assertThat(dbRule.getJdbi().onDemand(InterfaceDao.class)
                .defaultMethod()).isEqualTo(2288);
    }

    @GenerateSqlObject
    @GeneratorConfigurerType
    abstract static class AbstractClassDao implements SqlObject {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        abstract void insert(int id, String name);

        @GeneratorConfigurerMethod
        public boolean checkConfigurer(Predicate<GeneratorConfig> test) {
            final GeneratorConfig cfg = getHandle().getConfig(GeneratorConfig.class);
            return test.apply(cfg);
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

    @ConfiguringAnnotation(GeneratorConfigurerTypeImpl.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface GeneratorConfigurerType {}

    @ConfiguringAnnotation(GeneratorConfigurerMethodImpl.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface GeneratorConfigurerMethod {}

    public static class GeneratorConfigurerTypeImpl implements Configurer {
        @Override
        public void configureForType(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType) {
            registry.get(GeneratorConfig.class).configuredType = true;
        }
    }

    public static class GeneratorConfigurerMethodImpl implements Configurer {
        @Override
        public void configureForMethod(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType, Method method) {
            registry.get(GeneratorConfig.class).configuredMethod = true;
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
