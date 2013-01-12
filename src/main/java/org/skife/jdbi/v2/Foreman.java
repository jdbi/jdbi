package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.Argument;
import org.skife.jdbi.v2.tweak.ArgumentFactory;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

class Foreman
{

    private final List<ArgumentFactory> factories = new CopyOnWriteArrayList<ArgumentFactory>();

    public Foreman()
    {
        factories.add(BUILT_INS);
    }

    public Foreman(List<ArgumentFactory> factories)
    {
        this.factories.addAll(factories);
    }

    Argument waffle(Class expectedType, Object it, StatementContext ctx)
    {
        for (int i = factories.size() - 1; i >= 0; i--) {
            ArgumentFactory factory = factories.get(i);
            if (factory.accepts(expectedType, it, ctx)) {
                return factory.build(expectedType, it, ctx);
            }
        }
        throw new IllegalStateException("Unbindable argument passed: " + String.valueOf(it));
    }

    private static final ArgumentFactory BUILT_INS = new BuiltInArgumentFactory();

    public void register(ArgumentFactory<?> argumentFactory)
    {
        factories.add(argumentFactory);
    }

    public Foreman createChild()
    {
        return new Foreman(factories);
    }

    private static final class BuiltInArgumentFactory implements ArgumentFactory
    {
        private static final Map<Class, P> inheritedMap = new HashMap<Class, P>(){
            @Override
            public P get(Object key) {
                P p = super.get(key);
                if(p == null && key instanceof Class){
                    Class keyClass = (Class) key;
                    Class superclass = keyClass.getSuperclass();
                    while( superclass != null){
                        p = super.get(superclass);
                        if(p != null){
                            // fast access for future calls
                            put(keyClass, p);
                            break;
                        }
                        superclass = superclass.getSuperclass();
                    }
                }
                return p;
            }

            @Override
            public boolean containsKey(Object key) {
                return get(key) != null;
            }
        };

        private static final Map<Class, P> b = new IdentityHashMap<Class, P>();

        static {
            b.put(BigDecimal.class, new P(BigDecimalArgument.class));
            b.put(Blob.class, new P(BlobArgument.class));
            b.put(Boolean.class, new P(BooleanArgument.class));
            b.put(boolean.class, new P(BooleanArgument.class));
            b.put(Byte.class, new P(ByteArgument.class));
            b.put(byte.class, new P(ByteArgument.class));
            b.put(byte[].class, new P(ByteArrayArgument.class));
            b.put(Character.class, new P(CharacterArgument.class));
            b.put(char.class, new P(CharacterArgument.class));
            b.put(Clob.class, new P(ClobArgument.class));
            b.put(Double.class, new P(DoubleArgument.class));
            b.put(double.class, new P(DoubleArgument.class));
            b.put(Float.class, new P(FloatArgument.class));
            b.put(float.class, new P(FloatArgument.class));
            b.put(Integer.class, new P(IntegerArgument.class));
            b.put(int.class, new P(IntegerArgument.class));
            b.put(java.util.Date.class, new P(JavaDateArgument.class));
            b.put(Long.class, new P(LongArgument.class));
            b.put(long.class, new P(LongArgument.class));
            b.put(Object.class, new P(ObjectArgument.class));
            b.put(Short.class, new P(ShortArgument.class));
            b.put(short.class, new P(ShortArgument.class));
            b.put(java.sql.Date.class, new P(SqlDateArgument.class));
            b.put(String.class, new P(StringArgument.class));
            b.put(Time.class, new P(TimeArgument.class));
            b.put(Timestamp.class, new P(TimestampArgument.class));
            b.put(URL.class, new P(URLArgument.class));

            inheritedMap.put(Object.class, new P(ObjectArgument.class));
            inheritedMap.put(Enum.class, new P(EnumArgument.class));
        }

        public boolean accepts(Class expectedType, Object value, StatementContext ctx)
        {
            return b.containsKey(expectedType) || (inheritedMap.containsKey(expectedType));
        }

        public Argument build(Class expectedType, Object value, StatementContext ctx)
        {
            P p = b.get(expectedType);
            if(p == null){
                p = inheritedMap.get(expectedType);
            }
            return p.build(value);
        }

        private static class P
        {
            private final Constructor<?> ctor;

            public P(Class<? extends Argument> argType)
            {
                this.ctor = argType.getDeclaredConstructors()[0];
            }

            public Argument build(Object value)
            {
                try {
                    return (Argument) ctor.newInstance(value);
                }
                catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }


    }
}
