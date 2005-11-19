package org.skife.jdbi.tweak;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

/**
 * Script locator which takes a name and treats it as a URL, which it tries to open
 */
public class URLScriptLocator implements ScriptLocator
{
    /**
     * Treats name as a URL, which it attempts to open
     *
     * @param name URL name to extract script from.
     * @return open input stream, or null if nothing could be found.
     * @throws Exception if anything goes wrong locating the statement, will be wrapped in
     *                   a DBIException and rethrown
     */
    public InputStream locate(String name) throws Exception
    {
        try
        {
            return new URL(name).openStream();
        }
        catch (IOException e)
        {
            return null;
        }
    }
}
