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
package org.jdbi.v3.sqlobject.customizer;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.jdbi.v3.core.config.ConfigRegistry;

/**
 * Binds the properties of a JavaBean to a SQL statement.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@SqlStatementCustomizingAnnotation(BindBean.Factory.class)
public @interface BindBean
{
    /**
     * Prefix to apply to each bean property name. If specified, properties will be bound as
     * {@code prefix.propertyName}.
     */
    String value() default "";

    class Factory implements SqlStatementCustomizerFactory {
        @Override
        public SqlStatementParameterCustomizer createForParameter(ConfigRegistry registry,
                                                         Annotation annotation,
                                                         Class<?> sqlObjectType,
                                                         Method method,
                                                         Parameter param,
                                                         int index) {
            BindBean bind = (BindBean) annotation;
            return (stmt, bean) -> {
                String prefix = bind.value();
                if (prefix.isEmpty()) {
                    stmt.bindBean(bean);
                }
                else {
                    stmt.bindBean(prefix, bean);
                }
            };
        }
    }
}
