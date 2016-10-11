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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jdbi.v3.core.mapper.ConstructorMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.sqlobject.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.SqlStatementCustomizingAnnotation;

/**
 * Used to register a constructor mapper factory for the only constructor of a type.
 */
@SqlStatementCustomizingAnnotation(RegisterConstructorMapper.Factory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RegisterConstructorMapper
{
    Class<?>[] value();

    class Factory implements SqlStatementCustomizerFactory
    {

        @Override
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method)
        {
            return create((RegisterConstructorMapper) annotation);
        }

        @Override
        public SqlStatementCustomizer createForType(Annotation annotation, Class<?> sqlObjectType)
        {
            return create((RegisterConstructorMapper) annotation);
        }

        private SqlStatementCustomizer create(RegisterConstructorMapper rcm) {
            List<RowMapperFactory> mappers = Arrays.stream(rcm.value())
                    .map(ConstructorMapper::of)
                    .collect(Collectors.toList());

            return stmt -> mappers.forEach(stmt::registerRowMapper);
        }
    }
}
