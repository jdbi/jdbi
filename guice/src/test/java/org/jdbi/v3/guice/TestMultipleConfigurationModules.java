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
package org.jdbi.v3.guice;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import javax.sql.DataSource;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.guice.internal.JdbiGlobal;
import org.jdbi.v3.guice.util.GuiceTestSupport;
import org.junit.Test;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestMultipleConfigurationModules {

    static final QualifiedType<String> A_TYPE = QualifiedType.of(String.class).with(Names.named("a"));
    static final QualifiedType<Integer> B_TYPE = QualifiedType.of(Integer.class).with(Names.named("b"));

    @Test
    public void testGlobalModules() {

        Injector inj = GuiceTestSupport.createTestInjector(
            new AbstractJdbiConfigurationModule() {
                @Override
                public void configureJdbi() {
                    bindColumnMapper().to(DummyAMapper.class);
                    bindColumnMapper(A_TYPE).to(DummyAMapper.class);
                    bindCustomizer().toInstance(jdbi -> {});

                }
            },
            new AbstractJdbiConfigurationModule() {
                @Override
                public void configureJdbi() {
                    bindColumnMapper().to(DummyBMapper.class);
                    bindColumnMapper(B_TYPE).to(DummyBMapper.class);
                    bindCustomizer().toInstance(jdbi -> {});
                }
            }
        );
        assertNotNull(inj);
        Map<QualifiedType<?>, ColumnMapper<?>> qualifiedGlobalMappers = inj.getInstance(Key.get(new TypeLiteral<Map<QualifiedType<?>, ColumnMapper<?>>>() {},
            JdbiGlobal.class));

        assertEquals(2, qualifiedGlobalMappers.size());
        assertTrue(qualifiedGlobalMappers.containsKey(A_TYPE));
        assertTrue(qualifiedGlobalMappers.containsKey(B_TYPE));
        ColumnMapper<?> aMapper = qualifiedGlobalMappers.get(A_TYPE);
        ColumnMapper<?> bMapper = qualifiedGlobalMappers.get(B_TYPE);

        Set<ColumnMapper<?>> globalMappers = inj.getInstance(Key.get(new TypeLiteral<Set<ColumnMapper<?>>>() {}, JdbiGlobal.class));
        assertEquals(2, globalMappers.size());
        assertTrue(globalMappers.contains(aMapper));
        assertTrue(globalMappers.contains(bMapper));

        Set<GuiceJdbiCustomizer> customizers = inj.getInstance(Key.get(new TypeLiteral<Set<GuiceJdbiCustomizer>>() {}, JdbiGlobal.class));
        assertEquals(2, customizers.size());
    }

    @Retention(RUNTIME)
    @Target({FIELD, PARAMETER, METHOD})
    @Qualifier
    public @interface Foo {}

    @Test
    public void testLocalModules() {
        Injector inj = GuiceTestSupport.createTestInjector(
            new AbstractJdbiConfigurationModule(Foo.class) {
                @Override
                public void configureJdbi() {
                    bindColumnMapper().to(DummyAMapper.class);
                    bindColumnMapper(A_TYPE).to(DummyAMapper.class);
                    bindCustomizer().toInstance(jdbi -> {});

                }
            },
            new AbstractJdbiConfigurationModule(Foo.class) {
                @Override
                public void configureJdbi() {
                    bindColumnMapper().to(DummyBMapper.class);
                    bindColumnMapper(B_TYPE).to(DummyBMapper.class);
                    bindCustomizer().toInstance(jdbi -> {});
                }
            }
        );
        assertNotNull(inj);
        Map<QualifiedType<?>, ColumnMapper<?>> qualifiedMappers = inj.getInstance(Key.get(new TypeLiteral<Map<QualifiedType<?>, ColumnMapper<?>>>() {},
            Foo.class));

        assertEquals(2, qualifiedMappers.size());
        assertTrue(qualifiedMappers.containsKey(A_TYPE));
        assertTrue(qualifiedMappers.containsKey(B_TYPE));
        ColumnMapper<?> aMapper = qualifiedMappers.get(A_TYPE);
        ColumnMapper<?> bMapper = qualifiedMappers.get(B_TYPE);

        Set<ColumnMapper<?>> globalMappers = inj.getInstance(Key.get(new TypeLiteral<Set<ColumnMapper<?>>>() {}, Foo.class));
        assertEquals(2, globalMappers.size());
        assertTrue(globalMappers.contains(aMapper));
        assertTrue(globalMappers.contains(bMapper));

        Set<GuiceJdbiCustomizer> customizers = inj.getInstance(Key.get(new TypeLiteral<Set<GuiceJdbiCustomizer>>() {}, Foo.class));
        assertEquals(2, customizers.size());
    }

    @Test
    public void testDefinitionModule() {
        Injector inj = GuiceTestSupport.createTestInjector(
            binder -> {
                binder.bind(DataSource.class).annotatedWith(Foo.class).toInstance(new JdbcDataSource());
            },
            new AbstractJdbiConfigurationModule() {
                @Override
                public void configureJdbi() {
                    bindColumnMapper().to(DummyAMapper.class);
                    bindColumnMapper(A_TYPE).to(DummyAMapper.class);
                    bindCustomizer().toInstance(jdbi -> {});

                }
            },
            new AbstractJdbiDefinitionModule(Foo.class) {
                @Override
                public void configureJdbi() {
                    bindColumnMapper().to(DummyBMapper.class);
                    bindColumnMapper(B_TYPE).to(DummyBMapper.class);
                    bindCustomizer().toInstance(jdbi -> {});
                }
            }
        );
        assertNotNull(inj);
        Jdbi jdbi = inj.getInstance(Key.get(Jdbi.class, Foo.class));

        ColumnMappers mappers = jdbi.getConfig(ColumnMappers.class);

        assertTrue(mappers.findFor(A_TYPE).isPresent());
        assertTrue(mappers.findFor(B_TYPE).isPresent());
    }

    @Test
    public void testNestedModule() {
        Module definitionModule = new AbstractJdbiDefinitionModule(Foo.class) {
            @Override
            public void configureJdbi() {
                bindColumnMapper().to(DummyBMapper.class);
                bindColumnMapper(B_TYPE).to(DummyBMapper.class);
                bindCustomizer().toInstance(jdbi -> {});
            }
        };

        Module configModule = new AbstractJdbiConfigurationModule() {
            @Override
            public void configureJdbi() {
                bindColumnMapper().to(DummyAMapper.class);
                bindColumnMapper(A_TYPE).to(DummyAMapper.class);
                bindCustomizer().toInstance(jdbi -> {});
                // it is possible to nest a definition module in a configuration module,
                // as configuration modules are just regular modules while definition modules
                // are private modules. The opposite is *NOT* possible (as definition modules are private modules).
                install(definitionModule);

            }
        };

        Injector inj = GuiceTestSupport.createTestInjector(
            binder -> {
                binder.bind(DataSource.class).annotatedWith(Foo.class).toInstance(new JdbcDataSource());
            },
            configModule
        );

        assertNotNull(inj);
        Jdbi jdbi = inj.getInstance(Key.get(Jdbi.class, Foo.class));

        ColumnMappers mappers = jdbi.getConfig(ColumnMappers.class);

        assertTrue(mappers.findFor(A_TYPE).isPresent());
        assertTrue(mappers.findFor(B_TYPE).isPresent());
    }

    @Singleton
    public static class DummyAMapper implements ColumnMapper<String> {

        @Inject
        public DummyAMapper() {}

        @Override
        public String map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            return r.getString(columnNumber);
        }
    }

    @Singleton
    public static class DummyBMapper implements ColumnMapper<String> {

        @Inject
        public DummyBMapper() {}

        @Override
        public String map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            return r.getString(columnNumber);
        }
    }
}
