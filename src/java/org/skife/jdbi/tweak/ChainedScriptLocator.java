package org.skife.jdbi.tweak;

import java.io.InputStream;

/**
 * Will take a group of script locators and try each, in order, until one finds the requested resource.
 */
public class ChainedScriptLocator implements ScriptLocator
{
    private final ScriptLocator[] locators;

    public ChainedScriptLocator(ScriptLocator[] locators)
    {

        this.locators = locators;
    }

    /**
     * Tries each ScriptLocator passed to the constructor in order, returning from the first
     * which actually finds the requested resource.
     *
     * @param name Resource name to look for. This will be the raw value requested by the client.
     * @return open input stream, or null if nothing could be found.
     * @throws Exception if anything goes wrong locating the statement, will be wrapped in
     *                   a DBIException and rethrown
     */
    public InputStream locate(String name) throws Exception
    {
        for (int i = 0; i < locators.length; i++)
        {
            ScriptLocator locator = locators[i];
            InputStream in = locator.locate(name);
            if (in != null) return in;
        }
        return null;
    }
}
