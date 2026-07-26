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
package org.jdbi.sqlobject;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

import org.jdbi.core.Handle;
import org.jdbi.core.statement.Customizable;
import org.jdbi.core.statement.SqlStatements;
import org.jdbi.sqlobject.customizer.ConfigMutating;
import org.jdbi.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.sqlobject.customizer.SqlStatementCustomizingAnnotation;
import org.jdbi.sqlobject.customizer.SqlStatementParameterCustomizer;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A customizer that mutates configuration per invocation must declare {@link ConfigMutating} and is
 * then run on the classic per-statement path, where each invocation owns a private configuration copy.
 * This verifies the mutation takes effect and does not leak between invocations of a reused SQL object.
 */
public class TestConfigMutatingCustomizer {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
        handle.execute("create table something (id int primary key, name varchar(50))");
        handle.execute("insert into something (id, name) values (1, 'Alice')");
    }

    @Test
    public void mutationTakesEffectAndDoesNotLeakBetweenInvocations() {
        Dao dao = handle.attach(Dao.class);

        // Each call defines a different column through configuration; the classic path isolates the
        // mutation per statement, so repeated calls never observe another call's value.
        assertThat(dao.pick("name")).isEqualTo("Alice");
        assertThat(dao.pick("id")).isEqualTo("1");
        assertThat(dao.pick("name")).isEqualTo("Alice");
    }

    public interface Dao {
        @SqlQuery("select <col> from something where id = 1")
        String pick(@DefineViaConfig("col") String col);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @SqlStatementCustomizingAnnotation(DefineViaConfig.Factory.class)
    public @interface DefineViaConfig {
        String value();

        class Factory implements SqlStatementCustomizerFactory, ConfigMutating {
            @Override
            public SqlStatementParameterCustomizer createForParameter(
                    Annotation annotation, Class<?> sqlObjectType, Method method, Parameter param, int index, Type type) {
                final String name = ((DefineViaConfig) annotation).value();
                return new ConfigDefiner(name);
            }
        }
    }

    // A named class (not a lambda) so the handler can detect the ConfigMutating marker on the customizer.
    static final class ConfigDefiner implements SqlStatementParameterCustomizer, ConfigMutating {
        private final String name;

        ConfigDefiner(String name) {
            this.name = name;
        }

        @Override
        public void apply(Customizable<?> stmt, Object arg) {
            stmt.getConfig().get(SqlStatements.class).define(name, arg);
        }
    }
}
