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
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.sqlobject.ConfigurerFactory;
import org.jdbi.v3.sqlobject.ConfiguringAnnotation;

/**
 * Used to register an argument with either a sql object type or for a specific method.
 */
@ConfiguringAnnotation(RegisterArgument.Factory.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterArgument
{
    /**
     * The argument classes to register
     * @return one or more argument classes.
     */
    Class<? extends Argument>[] value();

    class Factory implements ConfigurerFactory
    {
        @Override
        public Consumer<ConfigRegistry> createForType(Annotation annotation, Class<?> sqlObjectType)
        {
            return create(annotation);
        }

        @Override
        public Consumer<ConfigRegistry> createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method)
        {
            return create(annotation);
        }

        private Consumer<ConfigRegistry> create(Annotation annotation)
        {
            final RegisterArgument raf = (RegisterArgument) annotation;
            final List<Argument> ary = new ArrayList<>(raf.value().length);
            for (Class<? extends Argument> aClass : raf.value()) {
                try {
                    ary.add(aClass.getDeclaredConstructor().newInstance());
                }
                catch (Exception e) {
                    throw new IllegalStateException("unable to instantiate specified argument", e);
                }
            }
            return config -> ary.forEach(config.get(Arguments.class)::register);
        }
    }
}
