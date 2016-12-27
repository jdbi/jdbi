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
package org.jdbi.v3.sqlobject.config;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.sql.Types;

import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.argument.ObjectArgumentFactory;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;

/**
 * Used to register argument factories for types which are compatible with
 * {@link java.sql.PreparedStatement#setObject(int, Object)}.
 */
@SqlStatementCustomizingAnnotation(RegisterObjectArgumentFactory.Factory.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterObjectArgumentFactory
{
    /**
     * The argument type(s) which are compatible with {@link java.sql.PreparedStatement#setObject(int, Object)}.
     * @return the argument types
     */
    Class<?>[] value();

    /**
     * SQL type constant(s) from {@link Types}. If omitted, defaults to not setting a type. If specified, must have the
     * same number of elements as {@link #value()}. Each <code>sqlType</code> element is applied to the
     * <code>value</code> element at the same index.
     * @return SQL types corresponding pairwise to the elements in {@link #value()}.
     */
    int[] sqlType() default {};

    class Factory implements SqlStatementCustomizerFactory
    {
        @Override
        public SqlStatementCustomizer createForType(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType) {
            RegisterObjectArgumentFactory roaf = (RegisterObjectArgumentFactory) annotation;
            Class<?>[] classes = roaf.value();
            int[] sqlTypes = roaf.sqlType();

            if (sqlTypes.length != 0 && sqlTypes.length != classes.length) {
                throw new IllegalStateException("RegisterObjectArgumentFactory.sqlTypes() must have the same number of elements as value()");
            }

            Arguments arguments = registry.get(Arguments.class);
            for (int i = 0; i < classes.length; i++) {
                Class<?> clazz = classes[i];
                Integer sqlType = sqlTypes.length == 0 ? null : sqlTypes[i];

                arguments.register(ObjectArgumentFactory.create(clazz, sqlType));
            }
            return NONE;
        }

        @Override
        public SqlStatementCustomizer createForMethod(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType, Method method) {
            return createForType(registry, annotation, sqlObjectType);
        }
    }
}
