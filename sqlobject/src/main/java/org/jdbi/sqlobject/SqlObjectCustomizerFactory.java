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
package org.jdbi.sqlobject;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import org.jdbi.core.config.ConfigCustomizer;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.extension.ConfigCustomizerFactory;
import org.jdbi.core.internal.JdbiClassUtils;
import org.jdbi.sqlobject.config.Configurer;
import org.jdbi.sqlobject.config.ConfiguringAnnotation;

class SqlObjectCustomizerFactory implements ConfigCustomizerFactory {

    static final ConfigCustomizerFactory FACTORY = new SqlObjectCustomizerFactory();

    @Override
    public Collection<ConfigCustomizer> forExtensionType(Class<?> sqlObjectType) {
        final ConfigurerMethod forType = (configurer, config, annotation) -> configurer.configureForType(config, annotation, sqlObjectType);

        // build a configurer for the type and all supertypes. This processes all annotations on classes and interfaces
        return buildConfigCustomizer(Stream.concat(JdbiClassUtils.superTypes(sqlObjectType), Stream.of(sqlObjectType)), forType);
    }

    @Override
    public Collection<ConfigCustomizer> forExtensionMethod(Class<?> sqlObjectType, Method method) {
        final ConfigurerMethod forMethod = (configurer, config, annotation) ->
                configurer.configureForMethod(config, annotation, sqlObjectType, method);
        // build a configurer that processes all annotations on the method itself.
        return buildConfigCustomizer(Stream.of(method), forMethod);
    }

    private static Collection<ConfigCustomizer> buildConfigCustomizer(Stream<AnnotatedElement> elements, ConfigurerMethod consumer) {
        return elements.flatMap(ae -> Arrays.stream(ae.getAnnotations()))
                .filter(a -> a.annotationType().isAnnotationPresent(ConfiguringAnnotation.class))
                .map(a -> {
                    ConfiguringAnnotation meta = a.annotationType().getAnnotation(ConfiguringAnnotation.class);
                    Configurer configurer = JdbiClassUtils.checkedCreateInstance(meta.value());
                    return (ConfigCustomizer) config -> consumer.configure(configurer, config, a);
                })
                .toList();
    }

    private interface ConfigurerMethod {

        void configure(Configurer configurer, ConfigRegistry config, Annotation annotation);
    }
}
