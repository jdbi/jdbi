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
package org.jdbi.v3.sqlobject.internal;

import java.lang.reflect.Parameter;
import java.util.Optional;

public class ParameterUtil {
    private ParameterUtil() {
        throw new UnsupportedOperationException("utility class");
    }

    public static Optional<String> findParameterName(String nameFromAnnotation,
                                                     Parameter parameter) {
        if (!nameFromAnnotation.isEmpty()) {
            return Optional.of(nameFromAnnotation);
        }
        return parameter.isNamePresent()
                ? Optional.of(parameter.getName())
                : Optional.empty();
    }
}
