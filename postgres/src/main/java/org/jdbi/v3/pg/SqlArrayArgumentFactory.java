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
package org.jdbi.v3.pg;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jdbi.v3.StatementContext;
import org.jdbi.v3.Types;
import org.jdbi.v3.argument.Argument;
import org.jdbi.v3.argument.ArgumentFactory;

public class SqlArrayArgumentFactory implements ArgumentFactory {

    @Override
    public Optional<Argument> build(Type type, Object value, StatementContext ctx) {
        return Types.getErasedType(type).isArray() ?
                Optional.of(ArrayArgument.fromAnyArray(guessSqlType(value), value)) :
                    Optional.empty();
    }

    private static final Map<Class<?>, String> BEST_GUESS;
    static {
        final Map<Class<?>, String> map = new IdentityHashMap<>();
        map.put(int.class, "integer");
        map.put(long.class, "bigint");
        map.put(String.class, "varchar");
        map.put(UUID.class, "uuid");
        map.put(float.class, "real");
        map.put(double.class, "double precision");
        BEST_GUESS = Collections.unmodifiableMap(map);
    }

    /**
     * Look at a Java array and attempt to determine an appropriate
     * SQL type to pass to the driver.
     */
    static final String guessSqlType(Object array) {
        final Class<?> klass = array.getClass();
        if (!klass.isArray()) {
            throw new IllegalArgumentException("not an array: " + klass);
        }
        String guess = BEST_GUESS.get(klass.getComponentType());
        if (((Object[]) array).length == 0 || guess == null) {
            return "varchar";
        }
        return guess;
    }
}
