package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.Argument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 */
public class Parameters
{
    private Map<Integer, Argument> positionals = new HashMap<Integer, Argument>();
    private Map<String, Argument> named = new HashMap<String, Argument>();
    private List<LazyArguments> lazyArguments = new ArrayList<LazyArguments>();

    void addPositional(int position, Argument parameter)
    {
        positionals.put(position, parameter);
    }

    public Argument forName(String name)
    {
        if (named.containsKey(name))
        {
            return named.get(name);
        }
        else
        {
            for (LazyArguments arguments : lazyArguments)
            {
                Argument arg = arguments.find(name);
                if (arg != null)
                {
                    return arg;
                }
            }
        }
        return null;
    }

    public Argument forPosition(int position)
    {
        return positionals.get(position);
    }

    void addNamed(String name, Argument argument)
    {
        this.named.put(name, argument);
    }

    void addLazyNamedArguments(LazyArguments args)
    {
        lazyArguments.add(args);
    }
}
