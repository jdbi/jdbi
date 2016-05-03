package org.jdbi.v3.pg;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jdbi.v3.StatementContext;
import org.jdbi.v3.Types;
import org.jdbi.v3.tweak.Argument;
import org.jdbi.v3.tweak.ArgumentFactory;

public class SqlArrayArgumentFactory implements ArgumentFactory {

    @Override
    public Optional<Argument> build(Type type, Object value, StatementContext ctx) {
        return Types.getErasedType(type).isArray() ?
                Optional.of(new ArrayArgument(guessSqlType((Object[]) value), value)) : Optional.empty();
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
    static final String guessSqlType(Object[] array) {
        String guess = BEST_GUESS.get(array.getClass().getComponentType());
        if (array.length == 0 || guess == null) {
            return "varchar";
        }
        return guess;
    }
}
