package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.tweak.Argument;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class Parameters
{
    private Map<Integer, Argument> positionals = new HashMap<Integer, Argument>();
    private Map<String, Argument> named = new HashMap<String, Argument>();

    public void addPositional(int position, Argument parameter)
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

    @Deprecated
    void apply(PreparedStatement statement)
    {
        for (Map.Entry<Integer, Argument> entry : positionals.entrySet())
        {
            try
            {
                entry.getValue().apply(entry.getKey() + 1, statement);
            }
            catch (SQLException e)
            {
                throw new UnableToExecuteStatementException(String.format("Error attempting " +
                                                                          "to bind argument, %s, to statement",
                                                                          entry.getValue()), e);
            }
        }
    }

    public void addNamed(String name, Argument argument)
    {
        this.named.put(name, argument);
    }
}
