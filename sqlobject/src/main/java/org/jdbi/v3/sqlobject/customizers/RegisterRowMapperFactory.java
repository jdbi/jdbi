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
import java.util.function.Consumer;

import org.jdbi.v3.core.ConfigRegistry;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.sqlobject.ConfigurerFactory;
import org.jdbi.v3.sqlobject.ConfiguringAnnotation;

/**
 * Used to register a row mapper factory with either a sql object type or for a specific method.
 */
@ConfiguringAnnotation(RegisterRowMapperFactory.Factory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RegisterRowMapperFactory
{
    Class<? extends RowMapperFactory>[] value();

    class Factory implements ConfigurerFactory
    {

        @Override
        public Consumer<ConfigRegistry> createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method)
        {
            return create((RegisterRowMapperFactory) annotation);
        }

        @Override
        public Consumer<ConfigRegistry> createForType(Annotation annotation, Class<?> sqlObjectType)
        {
            return create((RegisterRowMapperFactory) annotation);
        }

        private Consumer<ConfigRegistry> create(RegisterRowMapperFactory ma) {
            final List<RowMapperFactory> m = new ArrayList<RowMapperFactory>(ma.value().length);
            try {
                Class<? extends RowMapperFactory>[] mcs = ma.value();
                for (int i = 0; i < mcs.length; i++) {
                    m.add(mcs[i].newInstance());
                }

            }
            catch (Exception e) {
                throw new IllegalStateException("unable to create a specified row mapper factory", e);
            }
            return config -> m.forEach(config.get(RowMappers.class)::register);
        }
    }
}
