package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.Argument;
import org.skife.jdbi.v2.tweak.ArgumentFactory;

import java.sql.Types;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

class Foreman
{

    private final List<ArgumentFactory> factories = new ArrayList<ArgumentFactory>();
    {
        factories.add(new BuiltInArgumentFactory());
    }

    public Argument waffle(Class expectedType, Object it, StatementContext ctx)
    {
        for (ArgumentFactory factory : factories) {
            if (factory.accepts(expectedType, it, ctx)) {
                return factory.build(expectedType, it, ctx);
            }
        }
        throw new IllegalStateException("Unbindable argument passed: " + String.valueOf(it));
    }

    private static final class BuiltInArgumentFactory implements ArgumentFactory
    {

        private final Map<Class, PAF> primitives = new IdentityHashMap<Class, PAF>();

        {
            primitives.put(Integer.class, new IntegerPAF());
            primitives.put(int.class, new IntegerPAF());


        }

        public boolean accepts(Class expectedType, Object value, StatementContext ctx)
        {
            return primitives.containsKey(expectedType);
        }

        public Argument build(Class expectedType, Object value, StatementContext ctx)
        {
            return primitives.get(expectedType).build(value);
        }


        private static interface PAF<T>
        {
            Argument build(T value);
        }

        private static class IntegerPAF implements PAF<Integer>
        {
            public Argument build(Integer value)
            {
                if (value == null) {
                    return new NullArgument(Types.INTEGER);
                }
                else {
                    return new IntegerArgument(value);
                }
            }
        }
    }
}
