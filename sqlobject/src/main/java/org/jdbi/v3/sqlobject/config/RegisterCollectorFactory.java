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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jdbi.v3.core.collector.JdbiCollectors;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.collector.CollectorFactory;

/**
 * Used to register container mappers on the current {@link Query}
 * either for a sql object type or for a method.
 */
@ConfiguringAnnotation(RegisterCollectorFactory.Factory.class)
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterCollectorFactory {

    Class<? extends CollectorFactory>[] value();

    class Factory implements ConfigurerFactory {
        @Override
        public Consumer<ConfigRegistry> createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method) {
            return create((RegisterCollectorFactory) annotation);
        }

        @Override
        public Consumer<ConfigRegistry> createForType(Annotation annotation, Class<?> sqlObjectType) {
            return create((RegisterCollectorFactory) annotation);
        }

        private Consumer<ConfigRegistry> create(RegisterCollectorFactory annotation) {
            List<CollectorFactory> factories = new ArrayList<>();
            for (Class<? extends CollectorFactory> type : annotation.value()) {
                try {
                    factories.add(type.newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new IllegalStateException("Unable to instantiate container factory", e);
                }
            }

            return config -> factories.forEach(config.get(JdbiCollectors.class)::register);
        }
    }
}
