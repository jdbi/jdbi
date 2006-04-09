package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.tweak.StatementLocator;

import java.util.regex.Pattern;

/**
 * Represents a number of SQL statements which will be executed in a batch statement.
 */
public class Script
{

    private static final Pattern WHITESPACE_ONLY = Pattern.compile("^\\s*$");

    private Handle handle;
    private final StatementLocator locator;
    private final String name;

    Script(Handle h, StatementLocator locator, String name)
    {
        this.handle = h;
        this.locator = locator;
        this.name = name;
    }

    /**
     * Execute this script in a batch statement
     * 
     * @return an array of ints which are the results of each statement in the script
     */
    public int[] execute()
    {
        final String script;
        try
        {
            script = locator.locate(name);
        }
        catch (Exception e)
        {
            throw new UnableToExecuteStatementException(String.format("Error while loading script [%s]", name), e);
        }

        final String[] statements = script.replaceAll("\n", " ").replaceAll("\r", "").split(";");
        Batch b = handle.createBatch();
        for (String s : statements)
        {
            if ( ! WHITESPACE_ONLY.matcher(s).matches() ) {
                b.add(s);
            }
        }
        return b.execute();
    }
}
