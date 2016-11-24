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
package org.jdbi.v3.core;

import java.lang.reflect.Type;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.argument.NamedArgumentFinder;
import org.jdbi.v3.core.argument.NullValue;

/**
 * Binds all entries of a map as arguments.
 */
class MapArguments implements NamedArgumentFinder
{
    private final Map<String, ?> args;

    MapArguments(Map<String, ?> args)
    {
        this.args = args;
    }

    @Override
    public Optional<BoundArgument> find(String name)
    {
        if (args.containsKey(name)) {
            Object value = args.get(name);
            Type type;
            if (value == null) {
                value = new NullValue(Types.NULL);
                type = NullValue.class;
            }
            else {
                type = value.getClass();
            }
            return Optional.of(new BoundArgument(value, type));
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return new LinkedHashMap<>(args).toString();
    }
}
