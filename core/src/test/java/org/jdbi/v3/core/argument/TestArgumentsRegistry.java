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

import org.jdbi.v3.core.BoundArgument;
import org.jdbi.v3.core.ConfigRegistry;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.JdbiAccess;
import org.jdbi.v3.core.StatementContext;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class TestArgumentsRegistry
{
    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();

    private static final String I_AM_A_STRING = "I am a String";

    private final Handle handle = JdbiAccess.createHandle();
    private final StatementContext ctx = JdbiAccess.createContext(handle);

    @Mock
    public PreparedStatement stmt;

    @Test
    public void testExplicitWaffleLong() throws Exception {
        ctx.findArgumentFor(Long.class).get().apply(stmt, 1, 3L, null);
        verify(stmt).setLong(1, 3);
    }

    @Test
    public void testExplicitWaffleShort() throws Exception {
        ctx.findArgumentFor(short.class).get().apply(stmt, 2, (short) 2000, null);
        verify(stmt).setShort(2, (short) 2000);
    }

    @Test
    public void testExplicitWaffleString() throws Exception {
        ctx.findArgumentFor(String.class).get().apply(stmt, 3, I_AM_A_STRING, null);
        verify(stmt).setString(3, I_AM_A_STRING);
    }

    @Test
    public void testPull88WeirdClassArgumentFactory() throws Exception
    {
        handle.registerArgument(new WeirdClassArgumentFactory());

        assertThat(ctx.findArgumentFor(Weird.class))
                .hasValueSatisfying(a -> assertThat(a).isInstanceOf(WeirdArgument.class));

        assertThat(ctx.findArgumentFor(Object.class))
                .isEmpty();
    }

    private static class Weird
    {
    }

    private static class WeirdClassArgumentFactory implements ArgumentFactory
    {
        @Override
        public Optional<Argument> build(Type expectedType, ConfigRegistry config) {
            return getErasedType(expectedType) == Weird.class
                    ? Optional.of(new WeirdArgument())
                    : Optional.empty();
        }
    }

    private static class WeirdArgument implements Argument<Weird>
    {

        @Override
        public void apply(PreparedStatement statement, int position, Weird value, StatementContext ctx) throws SQLException
        {
        }
    }

    private void applyArgument(int position, BoundArgument argument) throws SQLException {
        ctx.findArgumentFor(argument.getType())
                .get()
                .apply(stmt, position, argument.getValue(), ctx);
    }
}
