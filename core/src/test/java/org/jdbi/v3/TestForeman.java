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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.fasterxml.classmate.ResolvedType;
import org.jdbi.v3.tweak.Argument;
import org.jdbi.v3.tweak.ArgumentFactory;
import org.junit.Test;

public class TestForeman
{
    @Test
    public void testWaffling()
    {
        final Foreman foreman = new Foreman();

        final Argument longArgument = foreman.waffle(Object.class, new Long(3L), null);
        assertSame(LongArgument.class, longArgument.getClass());

        final Argument shortArgument = foreman.waffle(Object.class, (short) 2000, null);
        assertSame(ShortArgument.class, shortArgument.getClass());

        final Argument stringArgument = foreman.waffle(Object.class, "I am a String!", null);
        assertSame(StringArgument.class, stringArgument.getClass());
    }

    @Test
    public void testExplicitWaffling()
    {
        final Foreman foreman = new Foreman();

        final Argument longArgument = foreman.waffle(Long.class, new Long(3L), null);
        assertSame(LongArgument.class, longArgument.getClass());

        final Argument shortArgument = foreman.waffle(short.class, (short) 2000, null);
        assertSame(ShortArgument.class, shortArgument.getClass());

        final Argument stringArgument = foreman.waffle(String.class, "I am a String!", null);
        assertSame(StringArgument.class, stringArgument.getClass());
    }

    @Test
    public void testPull88WeirdClassArgumentFactory()
    {
        final Foreman foreman = new Foreman();
        foreman.register(new WeirdClassArgumentFactory());

        // Pull Request #88 changes the outcome of this waffle call from ObjectArgument to WeirdArgument
        // when using SqlStatement#bind(..., Object) and the Object is != null
        assertEquals(WeirdArgument.class, foreman.waffle(Weird.class, new Weird(), null).getClass());

        assertEquals(ObjectArgument.class, foreman.waffle(Object.class, new Weird(), null).getClass());
    }

    @Test
    public void testPull88NullClassArgumentFactory()
    {
        final Foreman foreman = new Foreman();
        foreman.register(new WeirdClassArgumentFactory());

        assertEquals(WeirdArgument.class, foreman.waffle(Weird.class, null, null).getClass());
        assertEquals(ObjectArgument.class, foreman.waffle(Object.class, null, null).getClass());
    }

    @Test
    public void testPull88WeirdValueArgumentFactory()
    {
        final Foreman foreman = new Foreman();
        foreman.register(new WeirdValueArgumentFactory());

        // Pull Request #88 changes the outcome of this waffle call from ObjectArgument to WeirdArgument
        // when using SqlStatement#bind(..., Object) and the Object is != null
        assertEquals(WeirdArgument.class, foreman.waffle(Weird.class, new Weird(), null).getClass());
        assertEquals(WeirdArgument.class, foreman.waffle(Object.class, new Weird(), null).getClass());
    }

    @Test
    public void testPull88NullValueArgumentFactory()
    {
        final Foreman foreman = new Foreman();
        foreman.register(new WeirdValueArgumentFactory());

        assertEquals(ObjectArgument.class, foreman.waffle(Weird.class, null, null).getClass());
        assertEquals(ObjectArgument.class, foreman.waffle(Object.class, null, null).getClass());
    }

    private static class Weird
    {
    }

    private static class WeirdClassArgumentFactory implements ArgumentFactory<Weird>
    {
        @Override
        public boolean accepts(ResolvedType expectedType, Object value, StatementContext ctx)
        {
            return expectedType.getErasedType() == Weird.class;
        }

        @Override
        public Argument build(ResolvedType expectedType, Weird value, StatementContext ctx)
        {
            return new WeirdArgument();
        }
    }

    private static class WeirdValueArgumentFactory implements ArgumentFactory<Weird>
    {
        @Override
        public boolean accepts(ResolvedType expectedType, Object value, StatementContext ctx)
        {
            return value instanceof Weird;
        }

        @Override
        public Argument build(ResolvedType expectedType, Weird value, StatementContext ctx)
        {
            return new WeirdArgument();
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
