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
package org.jdbi.v3.core.argument;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.core.util.GenericTypes.getErasedType;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

import org.jdbi.v3.core.ArgumentRegistry;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.JdbiAccess;
import org.jdbi.v3.core.StatementContext;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class TestArgumentRegistry
{
    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();

    private static final String I_AM_A_STRING = "I am a String";

    private final Handle handle = JdbiAccess.createHandle();
    private final StatementContext ctx = JdbiAccess.createContext(handle);

    @Mock
    public PreparedStatement stmt;

    @Test
    public void testWaffleLong() throws Exception
    {
        ctx.getConfig(ArgumentRegistry.class).findArgumentFor(Object.class, 3L, ctx).get().apply(1, stmt, null);
        verify(stmt).setLong(1, 3);
    }

    @Test
    public void testWaffleShort() throws Exception
    {
        ctx.getConfig(ArgumentRegistry.class).findArgumentFor(Object.class, (short) 2000, ctx).get().apply(2, stmt, null);
        verify(stmt).setShort(2, (short) 2000);
    }

    @Test
    public void testWaffleString() throws Exception {
        ctx.getConfig(ArgumentRegistry.class).findArgumentFor(Object.class, I_AM_A_STRING, ctx).get().apply(3, stmt, null);
        verify(stmt).setString(3, I_AM_A_STRING);
    }

    @Test
    public void testExplicitWaffleLong() throws Exception {
        ctx.getConfig(ArgumentRegistry.class).findArgumentFor(Long.class, 3L, ctx).get().apply(1, stmt, null);
        verify(stmt).setLong(1, 3);
    }

    @Test
    public void testExplicitWaffleShort() throws Exception {
        ctx.getConfig(ArgumentRegistry.class).findArgumentFor(short.class, (short) 2000, ctx).get().apply(2, stmt, null);
        verify(stmt).setShort(2, (short) 2000);
    }

    @Test
    public void testExplicitWaffleString() throws Exception {
        ctx.getConfig(ArgumentRegistry.class).findArgumentFor(String.class, I_AM_A_STRING, ctx).get().apply(3, stmt, null);
        verify(stmt).setString(3, I_AM_A_STRING);
    }

    @Test
    public void testPull88WeirdClassArgumentFactory() throws Exception
    {
        handle.getConfig(ArgumentRegistry.class).register(new WeirdClassArgumentFactory());

        assertThat(ctx.getConfig(ArgumentRegistry.class).findArgumentFor(Weird.class, new Weird(), ctx))
                .hasValueSatisfying(a -> assertThat(a).isInstanceOf(WeirdArgument.class));
        assertThat(ctx.getConfig(ArgumentRegistry.class).findArgumentFor(Weird.class, null, ctx))
                .hasValueSatisfying(a -> assertThat(a).isInstanceOf(WeirdArgument.class));

        assertThat(ctx.getConfig(ArgumentRegistry.class).findArgumentFor(Object.class, new Weird(), ctx))
                .isEmpty();
        assertThat(ctx.getConfig(ArgumentRegistry.class).findArgumentFor(Object.class, null, ctx))
                .hasValueSatisfying(a -> assertThat(a).isInstanceOf(NullArgument.class));
    }

    @Test
    public void testPull88WeirdValueArgumentFactory()
    {
        handle.getConfig(ArgumentRegistry.class).register(new WeirdValueArgumentFactory());

        assertThat(ctx.getConfig(ArgumentRegistry.class).findArgumentFor(Weird.class, new Weird(), ctx))
                .hasValueSatisfying(a -> assertThat(a).isInstanceOf(WeirdArgument.class));
        assertThat(ctx.getConfig(ArgumentRegistry.class).findArgumentFor(Object.class, new Weird(), ctx))
                .hasValueSatisfying(a -> assertThat(a).isInstanceOf(WeirdArgument.class));

        assertThat(ctx.getConfig(ArgumentRegistry.class).findArgumentFor(Weird.class, null, ctx))
                .hasValueSatisfying(a -> assertThat(a).isInstanceOf(NullArgument.class));
        assertThat(ctx.getConfig(ArgumentRegistry.class).findArgumentFor(Object.class, null, ctx))
                .hasValueSatisfying(a -> assertThat(a).isInstanceOf(NullArgument.class));
    }

    private static class Weird
    {
    }

    private static class WeirdClassArgumentFactory implements ArgumentFactory
    {
        @Override
        public Optional<Argument> build(Type expectedType, Object value, StatementContext ctx) {
            return getErasedType(expectedType) == Weird.class
                    ? Optional.of(new WeirdArgument())
                    : Optional.empty();
        }
    }

    private static class WeirdValueArgumentFactory implements ArgumentFactory
    {
        @Override
        public Optional<Argument> build(Type expectedType, Object value, StatementContext ctx) {
            return value instanceof Weird
                    ? Optional.of(new WeirdArgument())
                    : Optional.empty();
        }
    }

    private static class WeirdArgument implements Argument
    {

        @Override
        public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException
        {
        }
    }
}
