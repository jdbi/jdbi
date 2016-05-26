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
import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.sqlobject.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.tweak.ArgumentFactory;

/**
 * Used to register an argument factory with either a sql object type or for a specific method.
 */
@SqlStatementCustomizingAnnotation(RegisterArgumentFactory.Factory.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterArgumentFactory
{
    /**
     * The argument factory classes to register
     * @return one or more argument factory classes.
     */
    Class<? extends ArgumentFactory>[] value();

    class Factory implements SqlStatementCustomizerFactory
    {
        @Override
        public SqlStatementCustomizer createForType(Annotation annotation, Class<?> sqlObjectType)
        {
            return create(annotation);
        }

        @Override
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method)
        {
            return create(annotation);
        }

        private SqlStatementCustomizer create(Annotation annotation)
        {
            final RegisterArgumentFactory raf = (RegisterArgumentFactory) annotation;
            final List<ArgumentFactory> ary = new ArrayList<>(raf.value().length);
            for (Class<? extends ArgumentFactory> aClass : raf.value()) {
                try {
                    ary.add(aClass.newInstance());
                }
                catch (Exception e) {
                    throw new IllegalStateException("unable to instantiate specified argument factory", e);
                }
            }
            return q -> ary.forEach(q::registerArgumentFactory);
        }
    }
}
