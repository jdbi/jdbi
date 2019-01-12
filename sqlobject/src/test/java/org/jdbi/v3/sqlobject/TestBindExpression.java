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

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.sqlobject.customizer.SqlStatementParameterCustomizer;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestBindExpression {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    @Test
    public void testExpression() {
        DB db = dbRule.getSharedHandle().attach(DB.class);
        db.insert(new Something(1, "syrup"), new Something(2, "whipped cream"));

        Something selected = db.findBySpecifier(new SyrupSpecifying());

        assertThat(selected).isEqualTo(new Something(1, "syrup"));
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface DB {
        @SqlBatch("insert into something (id, name) values(:id, :name)")
        void insert(@BindBean Something... things);

        @SqlQuery("select id, name from something where name = :breakfast.waffle.topping limit 1")
        Something findBySpecifier(@BindNameSpecifying("breakfast") SyrupSpecifying b);
    }

    private static class SyrupSpecifying {
        private String getNameValue() {
            return "syrup";
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @SqlStatementCustomizingAnnotation(NameSpecifyingCustomizerFactory.class)
    private @interface BindNameSpecifying {
        String value();
    }

    public static class NameSpecifyingCustomizerFactory implements SqlStatementCustomizerFactory {
        @Override
        public SqlStatementParameterCustomizer createForParameter(Annotation annotation,
                                                                  Class<?> sqlObjectType,
                                                                  Method method,
                                                                  Parameter param,
                                                                  int index,
                                                                  Type type) {
            String bindingName = ((BindNameSpecifying) annotation).value();
            assertThat(bindingName).isEqualTo("breakfast");

            return (stmt, specifier) -> stmt.bindNamedArgumentFinder((paramName, context) -> {
                assertThat(paramName).isEqualTo("breakfast.waffle.topping");

                SyrupSpecifying syrupSpecifier = (SyrupSpecifying) specifier;
                return Optional.of((position, statement, ctx) -> statement.setObject(position, syrupSpecifier.getNameValue()));
            });
        }
    }
}
