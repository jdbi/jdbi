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
package org.jdbi.v3.core.kotlin.internal;

import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;

import org.jdbi.v3.core.generic.GenericType;

final class TypeConversionHelper {
    static final Type GENERIC_STRING_LIST = new GenericType<List<String>>() {}.getType();

    static final Type GENERIC_THING = new GenericType<GenericThing<UUID>>() {}.getType();

    private TypeConversionHelper() {
        throw new AssertionError("TypeConversionHelper can not be instantiated");
    }

    static class GenericThing<T> {
        private final T value;

        GenericThing(T value) {
            this.value = value;
        }

        T getValue() {
            return value;
        }
    }
}
