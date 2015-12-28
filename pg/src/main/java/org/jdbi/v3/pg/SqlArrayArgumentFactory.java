package org.jdbi.v3.pg;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

import org.jdbi.v3.StatementContext;
import org.jdbi.v3.tweak.Argument;
import org.jdbi.v3.tweak.ArgumentFactory;

public class SqlArrayArgumentFactory implements ArgumentFactory<Object[]> {

    @Override
    public boolean accepts(Class<?> expectedType, Object value, StatementContext ctx) {
        return expectedType.isArray();
    }

    @Override
    public Argument build(Class<?> expectedType, Object[] value, StatementContext ctx) {
        return new ArrayArgument(guessSqlType(value), value);
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
