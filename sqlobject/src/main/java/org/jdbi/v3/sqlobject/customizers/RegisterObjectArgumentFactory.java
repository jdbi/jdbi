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
package org.jdbi.v3.sqlobject.customizers;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.argument.ObjectArgumentFactory;
import org.jdbi.v3.sqlobject.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.tweak.ArgumentFactory;

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
     * SQL type constant(s) from {@link Types}. If omitted, defaults to {@link Types#NULL}. If specified, must have the
     * same number of elements as {@link #value()}. Each <code>sqlType</code> element is applied to the
     * <code>value</code> element at the same index.
     * @return SQL types corresponding pairwise to the elements in {@link #value()}.
     */
    int[] sqlType() default {};

    class Factory implements SqlStatementCustomizerFactory
    {
        @Override
        public SqlStatementCustomizer createForType(Annotation annotation, Class<?> sqlObjectType)
        {
            return create((RegisterObjectArgumentFactory) annotation);
        }

        @Override
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method)
        {
            return create((RegisterObjectArgumentFactory) annotation);
        }

        private SqlStatementCustomizer create(RegisterObjectArgumentFactory annotation)
        {
            Class<?>[] classes = annotation.value();
            int[] sqlTypes = annotation.sqlType();

            if (sqlTypes.length != 0 && sqlTypes.length != classes.length) {
                throw new IllegalStateException("RegisterObjectArgumentFactory.sqlTypes() must have the same number of elements as value()");
            }

            List<ArgumentFactory> factories = new ArrayList<>(classes.length);
            for (int i = 0; i < classes.length; i++) {
                Class<?> clazz = classes[i];
                int sqlType = sqlTypes.length == 0 ? Types.NULL : sqlTypes[i];

                factories.add(ObjectArgumentFactory.create(clazz, sqlType));
            }

            return q -> factories.forEach(q::registerArgumentFactory);
        }
    }
}
