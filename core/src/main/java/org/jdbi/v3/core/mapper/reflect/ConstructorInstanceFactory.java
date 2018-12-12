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
package org.jdbi.v3.core.mapper.reflect;

import java.lang.reflect.Constructor;
import org.jdbi.v3.core.internal.exceptions.Unchecked;

import static java.util.Objects.requireNonNull;

class ConstructorInstanceFactory<T> extends InstanceFactory<T> {
    private final Constructor<T> constructor;

    ConstructorInstanceFactory(Constructor<T> constructor) {
        super(constructor);
        this.constructor = requireNonNull(constructor, "constructor is null");
    }

    @Override
    T newInstance(Object... params) {
        return Unchecked.<Object[], T>function(constructor::newInstance).apply(params);
    }

    @Override
    public String toString() {
        return constructor.toString();
    }
}
