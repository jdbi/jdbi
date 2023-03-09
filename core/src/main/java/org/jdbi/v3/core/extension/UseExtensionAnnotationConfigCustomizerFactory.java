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
package org.jdbi.v3.core.extension;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdbi.v3.core.config.ConfigCustomizer;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.annotation.UseExtensionConfigurer;
import org.jdbi.v3.core.internal.JdbiClassUtils;

import static java.lang.String.format;

/**
 * Applies configuration customizers according to {@link UseExtensionConfigurer} decorating annotations.
 * present on the method.
 */
final class UseExtensionAnnotationConfigCustomizerFactory implements ConfigCustomizerFactory {

    static final ConfigCustomizerFactory FACTORY = new UseExtensionAnnotationConfigCustomizerFactory();

    @Override
    public Collection<ConfigCustomizer> forExtensionType(Class<?> extensionType) {
        final ConfigurerMethod forType = (configurer, config, annotation) -> configurer.configureForType(config, annotation, extensionType);

        // build a configurer for the type and all supertypes. This processes all annotations on classes and interfaces
        return buildConfigCustomizer(Stream.concat(JdbiClassUtils.superTypes(extensionType), Stream.of(extensionType)), forType);
    }

    @Override
    public Collection<ConfigCustomizer> forExtensionMethod(Class<?> extensionType, Method method) {
        final ConfigurerMethod forMethod = (configurer, config, annotation) -> configurer.configureForMethod(config, annotation, extensionType, method);
        // build a configurer that processes all annotations on the method itself.
        return buildConfigCustomizer(Stream.of(method), forMethod);
    }

    private static Collection<ConfigCustomizer> buildConfigCustomizer(Stream<AnnotatedElement> elements, ConfigurerMethod consumer) {
        return elements.flatMap(ae -> Arrays.stream(ae.getAnnotations()))
                .filter(a -> a.annotationType().isAnnotationPresent(UseExtensionConfigurer.class))
                .map(a -> {
                    UseExtensionConfigurer meta = a.annotationType().getAnnotation(UseExtensionConfigurer.class);
                    Class<? extends ExtensionConfigurer> klass = meta.value();

                    try {
                        ExtensionConfigurer configurer = klass.getConstructor().newInstance();
                        return (ConfigCustomizer) config -> consumer.configure(configurer, config, a);
                    } catch (ReflectiveOperationException | SecurityException e) {
                        throw new IllegalStateException(format("Unable to instantiate class %s", klass), e);
                    }

                })
                .collect(Collectors.toList());
    }

    private interface ConfigurerMethod {

        void configure(ExtensionConfigurer configurer, ConfigRegistry config, Annotation annotation);
    }
}
