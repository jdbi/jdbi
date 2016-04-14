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

import org.jdbi.v3.Query;
import org.jdbi.v3.sqlobject.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.tweak.BeanMapperFactory;

@Retention(RetentionPolicy.RUNTIME)
@SqlStatementCustomizingAnnotation(RegisterBeanMapper.Factory.class)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RegisterBeanMapper
{
    /**
     * The bean classes to map with BeanMapper.
     */
    Class<?>[] value();

    class Factory implements SqlStatementCustomizerFactory
    {
        @Override
        public SqlStatementCustomizer createForType(Annotation annotation, Class<?> sqlObjectType) {
            return create((RegisterBeanMapper) annotation);
        }

        @Override
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method)
        {
            return create((RegisterBeanMapper) annotation);
        }

        private SqlStatementCustomizer create(RegisterBeanMapper annotation) {
            return statement -> {
                Query<?> query = (Query<?>) statement;
                query.registerRowMapper(new BeanMapperFactory(annotation.value()));
            };
        }
    }
}
