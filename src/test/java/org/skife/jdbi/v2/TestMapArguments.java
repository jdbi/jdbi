package org.skife.jdbi.v2;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.skife.jdbi.v2.tweak.Argument;

public class TestMapArguments extends TestCase
{
    public void testSimple()
    {
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("foo", "foo");
        map.put("bar", 12345L);
        map.put("bool", new BooleanArgument(true));

        MapArguments args = new MapArguments(map);

        assertNotNull(args.find("foo"));
        assertNotNull(args.find("bar"));
        assertNotNull(args.find("bool"));

        assertTrue(args.find("foo") instanceof Argument);
        assertTrue(args.find("bar") instanceof Argument);
        assertTrue(args.find("bool") instanceof Argument);

        Argument boolArg = args.find("bool");

        assertTrue(boolArg instanceof BooleanArgument);
    }

    public void testNull()
    {
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("foo", null);

        MapArguments args = new MapArguments(map);

        assertNotNull(args.find("foo"));

        assertTrue(args.find("foo") instanceof Argument);

        Argument nullArg = args.find("foo");

        assertTrue(nullArg instanceof NullArgument);
    }
}

