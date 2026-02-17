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
package org.jdbi.guice;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.core.Jdbi;
import org.jdbi.core.mapper.ColumnMapper;
import org.jdbi.core.mapper.ColumnMappers;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.core.statement.StatementContext;
import org.jdbi.guice.util.GuiceTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import static com.google.inject.name.Names.named;
import static org.assertj.core.api.Assertions.assertThat;

public class TestCustomConfigurationModule {

    public static final String GLOBAL = "global";
    public static final String LOCAL = "local";
    public static final String CUSTOM = "custom";

    @Inject
    @Named("test")
    public Jdbi jdbi;

    @BeforeEach
    public void setUp() throws Exception {
        DataSource ds = new JdbcDataSource();

        Injector inj = GuiceTestSupport.createTestInjector(
            binder -> binder.bind(DataSource.class).annotatedWith(named("test")).toInstance(ds),

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
        assertThat(customMapper.map(null, 1, null)).isEqualTo(CUSTOM);

        // explicit local mapper from the local module
        ColumnMapper<String> localMapper = columnMappers.findFor(QualifiedType.of(String.class).with(named("local")))
            .orElseThrow(IllegalStateException::new);
        assertThat(localMapper.map(null, 1, null)).isEqualTo(LOCAL);

        // but the global mapper is not loaded
        assertThat(columnMappers.findFor(QualifiedType.of(String.class).with(named("global")))).isNotPresent();
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
    @javax.inject.Qualifier
    @jakarta.inject.Qualifier
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
