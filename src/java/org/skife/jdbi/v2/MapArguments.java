package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.Argument;

import java.util.Map;

/**
 * 
 */
class MapArguments implements LazyArguments
{
    private final Map<String, ? extends Object> args;

    MapArguments(Map<String, ? extends Object> args)
    {

        this.args = args;
    }

    public Argument find(String name)
    {
        if (args.containsKey(name))
        {
            return new ObjectArgument(args.get(name));
        }
        else
        {
            return null;
        }
    }
}
