package org.skife.jdbi.tweak;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;

/**
 * Looks on the file system for an SQL script.
 */
public class FileSystemScriptLocator implements ScriptLocator
{
    /**
     * The input stream should read the script. The input stream will be closed after it
     * reaches the end of file.
     *
     * @param name Resource name to look for. This will be the raw value requested by the client.
     * @return open input stream, or null if nothing could be found.
     * @throws Exception if anything goes wrong locating the statement, will be wrapped in
     *                   a DBIException and rethrown
     */
    public InputStream locate(String name) throws Exception
    {
        File literal = new File(name);
        if (literal.exists())
        {
            return new FileInputStream(literal);
        }
        return null;
    }
}
