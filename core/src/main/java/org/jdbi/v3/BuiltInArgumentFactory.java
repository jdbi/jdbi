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
package org.jdbi.v3;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import org.jdbi.v3.tweak.Argument;
import org.jdbi.v3.tweak.ArgumentFactory;

public class BuiltInArgumentFactory implements ArgumentFactory {

    public static final ArgumentFactory<?> INSTANCE = new BuiltInArgumentFactory();

    private static final Map<Class<?>, P> b = new IdentityHashMap<>();

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
    }

    @Override
    public boolean accepts(ResolvedType expectedType, Object value, StatementContext ctx)
    {
        return b.containsKey(expectedType.getErasedType())
                || value == null
                || value.getClass().isEnum()
                || value instanceof Optional;
    }

    @Override
    public Argument build(ResolvedType expectedType, Object value, StatementContext ctx)
    {
        Class<?> expectedClass = expectedType.getErasedType();

        if (value != null && expectedClass == Object.class) {
            expectedClass = value.getClass();
        }

        if (b.containsKey(expectedClass)) {
            return b.get(expectedClass).build(value);
        }

        // Enums must be bound as VARCHAR.
        if (value instanceof Enum) {
            return new StringArgument(((Enum)value).name());
        }

        if (value instanceof Optional) {
            Object nestedValue = ((Optional)value).orElse(null);
            ResolvedType nestedType = findOptionalType(expectedType, nestedValue);
            return build(nestedType, nestedValue, ctx);
        }

        // Fallback to generic ObjectArgument
        return new ObjectArgument(value);
    }

    private ResolvedType findOptionalType(ResolvedType wrapperType, Object nestedValue) {
        if (wrapperType.getErasedType().equals(Optional.class)) {
            List<ResolvedType> typeParameters = wrapperType.typeParametersFor(Optional.class);
            if (!typeParameters.isEmpty()) {
                return typeParameters.get(0);
            }
        }
        return new TypeResolver().resolve(nestedValue == null ? Object.class : nestedValue.getClass());
    }

    private static class P
    {
        private final Constructor<?> ctor;
        P(Class<? extends Argument> argType)
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
