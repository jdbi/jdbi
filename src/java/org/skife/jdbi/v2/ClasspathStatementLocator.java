package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.UnableToCreateStatementException;
import org.skife.jdbi.v2.tweak.StatementLocator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * looks for [name], then [name].sql on the classpath
 */
public class ClasspathStatementLocator implements StatementLocator
{
    private boolean looksLikeSql(String sql)
    {
        final String local = sql.substring(0, 7).toLowerCase();
        return local.startsWith("insert ")
               || local.startsWith("update ")
               || local.startsWith("select ")
               || local.startsWith("call ")
               || local.startsWith("delete ")
               || local.startsWith("create ")
               || local.startsWith("alter ")
               || local.startsWith("drop ");
    }

    public String locate(String name) throws Exception
    {
        if (looksLikeSql(name))
        {
            return name;
        }
        final ClassLoader loader = selectClassLoader();
        InputStream in_stream = loader.getResourceAsStream(name);
        if (in_stream == null)
        {
            in_stream = loader.getResourceAsStream(name + ".sql");
        }
        if (in_stream == null)
        {
            return name;
        }
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in_stream));
        final StringBuffer buffer = new StringBuffer();
        String line;
        try
        {
            while ((line = reader.readLine()) != null)
            {
                if (isComment(line))
                {
                    // comment
                    continue;
                }
                buffer.append(line).append(" ");
            }
            reader.close();
        }
        catch (IOException e)
        {
            throw new UnableToCreateStatementException(e.getMessage(), e);
        }
        return buffer.toString();
    }

    /**
     * There *must* be a better place to put this without creating a util class just for it
     */
    public static ClassLoader selectClassLoader()
    {
        ClassLoader loader;
        if (Thread.currentThread().getContextClassLoader() != null)
        {
            loader = Thread.currentThread().getContextClassLoader();
        }
        else
        {
            loader = ClasspathStatementLocator.class.getClassLoader();
        }
        return loader;
    }

    public static boolean isComment(final String line)
    {
        return line.startsWith("#") || line.startsWith("--") || line.startsWith("//");
    }
}
