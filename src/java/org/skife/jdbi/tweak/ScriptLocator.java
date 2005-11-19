package org.skife.jdbi.tweak;

import java.io.InputStream;

/**
 * Mechanism for locating SQL scripts
 */
public interface ScriptLocator
{
    /**
     * The input stream should read the script. The input stream will be closed after it
     * reaches the end of file. 
     *
     * @param name Resource name to look for. This will be the raw value requested by the client.
     * @return open input stream, or null if nothing could be found.
     * @throws Exception if anything goes wrong locating the statement, will be wrapped in
     *         a DBIException and rethrown
     */
    InputStream locate(String name) throws Exception;
}
