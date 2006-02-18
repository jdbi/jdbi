package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.Argument;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class Parameters
{
    private Map<Integer, Argument> positionals = new HashMap<Integer, Argument>();
    private Map<String, Argument> named = new HashMap<String, Argument>();

    void addPositional(int position, Argument parameter)
    {
        positionals.put(position, parameter);
    }

    public Argument forName(String name)
    {
        return named.get(name);
    }

    public Argument forPosition(int position)
    {
        return positionals.get(position);
    }
    
    void addNamed(String name, Argument argument)
    {
        this.named.put(name, argument);
    }
}
