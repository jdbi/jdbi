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
package org.jdbi.v3.core.mapper;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Holder for a single joined row.
 */
public class JoinRow
{
    private final Map<Type, Object> entries;

    JoinRow(Map<Type, Object> entries) {
        this.entries = entries;
    }

    /**
     * Return the value mapped for a given class.
     *
     * @param <T> the type to map
     * @param klass the type that was mapped
     * @return the value for that type
     */
    public <T> T get(Class<T> klass) {
        return klass.cast(get((Type)klass));
    }

    /**
     * Return the value mapped for a given type.
     * @param type the type that was mapped
     * @return the value for that type
     */
    public Object get(Type type) {
        Object result = entries.get(type);
        if (result == null && !entries.containsKey(type)) {
            throw new IllegalArgumentException("no result stored for " + type);
        }
        return result;
    }
}
