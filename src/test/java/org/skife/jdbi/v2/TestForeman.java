package org.skife.jdbi.v2;

import org.junit.Test;
import org.skife.jdbi.v2.tweak.Argument;

import static org.junit.Assert.assertSame;

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

}
