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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.sql.DataSource;

import com.google.inject.Injector;
import com.opentable.db.postgres.junit.EmbeddedPostgresRules;
import com.opentable.db.postgres.junit.SingleInstancePostgresRule;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.guice.util.GuiceTestSupport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import static com.google.inject.name.Names.named;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TestCustomConfigurationModule {

    public static final String GLOBAL = "global";
    public static final String LOCAL = "local";
    public static final String CUSTOM = "custom";

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    @Inject
    @Named("test")
    public Jdbi jdbi;

    @Before
    public void setUp() throws Exception {
        Injector inj = GuiceTestSupport.createTestInjector(
            binder -> binder.bind(DataSource.class).annotatedWith(named("test")).toInstance(pg.getEmbeddedPostgres().getPostgresDatabase()),

            new InstanceModule(),
            new CustomModule(),
            new GlobalModule());
        inj.injectMembers(this);
    }

    @Test
    public void testOnlyCustomModule() throws Exception {
        final ColumnMappers columnMappers = jdbi.getConfig().get(ColumnMappers.class);

        // explicit custom mapper from the custom module
        ColumnMapper<String> customMapper = columnMappers.findFor(QualifiedType.of(String.class).with(named("custom")))
            .orElseThrow(IllegalStateException::new);
        assertEquals(CUSTOM, customMapper.map(null, 1, null));

        // explicit local mapper from the local module
        ColumnMapper<String> localMapper = columnMappers.findFor(QualifiedType.of(String.class).with(named("local")))
            .orElseThrow(IllegalStateException::new);
        assertEquals(LOCAL, localMapper.map(null, 1, null));

        // but the global mapper is not loaded
        assertFalse(columnMappers.findFor(QualifiedType.of(String.class).with(named("global"))).isPresent());
    }

    static class GlobalModule extends AbstractJdbiConfigurationModule {

        @Override
        public void configureJdbi() {
            bindColumnMapper().toInstance(new TestingColumnMapper(GLOBAL));
            bindColumnMapper(QualifiedType.of(String.class).with(named(GLOBAL))).toInstance(new TestingColumnMapper(GLOBAL));
            bindColumnMapper(QualifiedType.of(String.class).with(named("qualified"))).toInstance(new TestingColumnMapper(GLOBAL));
        }
    }

    @Retention(RUNTIME)
    @Target({FIELD, PARAMETER, METHOD})
    @Qualifier
    public @interface CustomTest {}

    static class CustomModule extends AbstractJdbiConfigurationModule {

        CustomModule() {
            super(CustomTest.class);
        }

        @Override
        public void configureJdbi() {
            bindColumnMapper().toInstance(new TestingColumnMapper(CUSTOM));
            bindColumnMapper(QualifiedType.of(String.class).with(named(CUSTOM))).toInstance(new TestingColumnMapper(CUSTOM));
            bindColumnMapper(QualifiedType.of(String.class).with(named("qualified"))).toInstance(new TestingColumnMapper(CUSTOM));
        }
    }

    static class InstanceModule extends AbstractJdbiDefinitionModule {

        InstanceModule() {
            super(named("test"), CustomTest.class);
        }

        @Override
        public void configureJdbi() {
            bindColumnMapper().toInstance(new TestingColumnMapper(LOCAL));

            bindColumnMapper(QualifiedType.of(String.class).with(named("local"))).toInstance(new TestingColumnMapper(LOCAL));
            bindColumnMapper(QualifiedType.of(String.class).with(named("qualified"))).toInstance(new TestingColumnMapper(LOCAL));
        }
    }

    private static class TestingColumnMapper implements ColumnMapper<String> {

        private final String mode;

        TestingColumnMapper(String mode) {
            this.mode = mode;
        }

        @Override
        public String map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            return mode;
        }
    }
}
