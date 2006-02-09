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
class Parameters
{
    private Map<Integer, Argument> positionals = new HashMap<Integer, Argument>();

    public void addPositional(int position, Argument parameter)
    {
        positionals.put(position, parameter);
    }

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
}
