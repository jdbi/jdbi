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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jdbi.v3.core.extension.ExtensionHandler.ExtensionHandlerFactory;

/**
 * Extension handler factory for bridge methods. Forwards bridge methods to matching candidates.
 *
 * @since 3.38.0
 */
final class BridgeMethodExtensionHandlerFactory implements ExtensionHandlerFactory {

    static final ExtensionHandlerFactory INSTANCE = new BridgeMethodExtensionHandlerFactory();

    @Override
    public boolean accepts(Class<?> extensionType, Method method) {
        return method.isBridge();
    }

    @Override
    public Optional<ExtensionHandler> buildExtensionHandler(Class<?> extensionType, Method method) {

        List<Method> candidates = Stream.of(extensionType.getMethods())
                .filter(candidate -> !candidate.isBridge())
                .filter(candidate -> Objects.equals(candidate.getName(), method.getName()))
                .filter(candidate -> candidate.getParameterCount() == method.getParameterCount())
                .filter(candidate -> {
                    Class<?>[] candidateParamTypes = candidate.getParameterTypes();
                    Class<?>[] methodParamTypes = method.getParameterTypes();
                    return IntStream.range(0, method.getParameterCount())
                            .allMatch(i -> methodParamTypes[i].isAssignableFrom(candidateParamTypes[i]));
                })
                .collect(Collectors.toList());

        for (Method candidate : candidates) {
            try {
                return Optional.of(ExtensionHandler.createForMethod(candidate));
            } catch (IllegalAccessException ignored) {}
        }
        throw new UnableToCreateExtensionException(
                "Could not create an extension handler for bridge method %s#%s.", extensionType, method);
    }
}
