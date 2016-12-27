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

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;

/**
 * Used to register a constructor mapper factory for the only constructor of a type.
 */
@SqlStatementCustomizingAnnotation(RegisterConstructorMapper.Factory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RegisterConstructorMapper
{
    /**
     * The type to map with ConstructorMapper.
     * @return one or more mapped types.
     */
    Class<?>[] value();

    /**
     * Column name prefix for each mapped type. If omitted, defaults to no prefix. If specified, must have the same
     * number of elements as {@link #value()}. Each <code>prefix</code> element is applied to the <code>value</code>
     * element at the same index.
     *
     * @return Column name prefixes corresponding pairwise to the elements in {@link #value()}.
     */
    String[] prefix() default {};

    class Factory implements SqlStatementCustomizerFactory
    {
        @Override
        public SqlStatementCustomizer createForType(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType) {
            RegisterConstructorMapper rcm = (RegisterConstructorMapper) annotation;
            Class<?>[] types = rcm.value();
            String[] prefixes = rcm.prefix();
            RowMappers mappers = registry.get(RowMappers.class);
            if (prefixes.length == 0) {
                for (Class<?> type : types) {
                    mappers.register(ConstructorMapper.of(type));
                }
            }
            else if (prefixes.length == types.length) {
                for (int i = 0; i < types.length; i++) {
                    mappers.register(ConstructorMapper.of(types[i], prefixes[i]));
                }
            }
            else {
                throw new IllegalStateException("RegisterConstructorMapper.prefix() must have the same number of elements as value()");
            }
            return NONE;
        }

        @Override
        public SqlStatementCustomizer createForMethod(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType, Method method) {
            return createForType(registry, annotation, sqlObjectType);
        }
    }
}
