package org.skife.jdbi.tweak;

import java.io.InputStream;

/**
 * Looks for a script on the classpath. Will first try the exact name passed, will
 * then try appending ".sql" if the name doesn't already end in .sql.
 * <p/>
 * The suffix mechanism will probably be removed in an upcoming major release, it
 * is included here for backwards compatibility with earlier script locating
 * mechanisms.
 */
public class ClasspathScriptLocator implements ScriptLocator
{
    /**
     * Looks for a script on the classpath. Will first try the exact name passed, will
     * then try appending ".sql" if the name doesn't already end in .sql.
     * <p/>
     * The suffix mechanism will probably be removed in an upcoming major release, it
     * is included here for backwards compatibility with earlier script locating
     * mechanisms.
     *
     * @param name Resource name to look for. This will be the raw value requested by the client.
     * @return open input stream, or null if nothing could be found.
     * @throws Exception if anything goes wrong locating the statement, will be wrapped in
     *                   a DBIException and rethrown
     */
    public InputStream locate(String name) throws Exception
    {
        ClassLoader cl = ClasspathStatementLocator.selectClassLoader();
        InputStream in = cl.getResourceAsStream(name);
        if (in == null && ! name.endsWith(".sql"))
        {
            in = cl.getResourceAsStream(name + ".sql");
        }
        return in;
    }
}
