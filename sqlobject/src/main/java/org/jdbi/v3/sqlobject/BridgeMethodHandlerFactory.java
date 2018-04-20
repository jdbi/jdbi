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
package org.jdbi.v3.sqlobject;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class BridgeMethodHandlerFactory implements HandlerFactory {
    @Override
    public Optional<Handler> buildHandler(Class<?> sqlObjectType, Method method) {
        if (!method.isBridge()) {
            return Optional.empty();
        }

        return Stream.of(sqlObjectType.getMethods())
            .filter(candidate -> !candidate.isBridge()
                && Objects.equals(candidate.getName(), method.getName())
                && candidate.getParameterCount() == method.getParameterCount())
            .filter(candidate -> {
                Class<?>[] candidateParamTypes = candidate.getParameterTypes();
                Class<?>[] methodParamTypes = method.getParameterTypes();
                return IntStream.range(0, method.getParameterCount())
                    .allMatch(i -> methodParamTypes[i].isAssignableFrom(candidateParamTypes[i]));
            })
            .<Handler>map(m -> (target, args, handle) -> m.invoke(target, args))
            .findFirst();
    }
}
