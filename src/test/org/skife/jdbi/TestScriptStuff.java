package org.skife.jdbi;

import junit.framework.TestCase;
import org.skife.jdbi.derby.Tools;
import org.skife.jdbi.tweak.ClasspathScriptLocator;
import org.skife.jdbi.tweak.FileSystemScriptLocator;
import org.skife.jdbi.tweak.URLScriptLocator;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * 
 */
public class TestScriptStuff extends TestCase
{
    private Handle h;

    public void setUp() throws Exception
    {
        Tools.start();
        Tools.dropAndCreateSomething();
        this.h = DBI.open(Tools.CONN_STRING);

    }

    public void tearDown() throws Exception
    {
        Tools.stop();
        if (h.isOpen()) h.close();
    }

    public void testBasics() throws Exception
    {

    }

    public void testClasspathScriptLocatorWithExtension() throws Exception
    {
        ClasspathScriptLocator csl = new ClasspathScriptLocator();
        InputStream in = csl.locate("insert-script-with-comments.sql");
        assertNotNull(in);
        in.close();
    }

    public void testClasspathStatementLocatorReturnsNullOnNonExistantResource() throws Exception
    {
        ClasspathScriptLocator csl = new ClasspathScriptLocator();
        InputStream in = csl.locate("this-doesn't-exist");
        assertNull(in);
    }

    public void testClasspathStatementLocatorWithoutSQLExtension() throws Exception
    {
        ClasspathScriptLocator csl = new ClasspathScriptLocator();
        InputStream in = csl.locate("insert-script-with-comments");
        assertNotNull(in);
        in.close();
    }

    public void testFileSystemScriptLoaderWithExtension() throws Exception
    {
        FileSystemScriptLocator fsl = new FileSystemScriptLocator();
        InputStream in = fsl.locate("src/test-etc/insert-script-with-comments.sql");
        assertNotNull(in);
        in.close();
    }

    public void testFileSystemScriptLoaderWithInvalidResource() throws Exception
    {
        FileSystemScriptLocator fsl = new FileSystemScriptLocator();
        InputStream in = fsl.locate("src/test-etc/i-do-not-exist");
        assertNull(in);
    }

    public void testURLScriptLocatorFileScheme() throws Exception
    {
        File script = new File(System.getProperty("user.dir") + "/src/test-etc/insert-script-with-comments.sql");
        URL url = script.toURL();

        URLScriptLocator usl = new URLScriptLocator();
        InputStream in = usl.locate(url.toExternalForm());
        assertNotNull(in);
        in.close();
    }

    public void testURLScriptLocatorFileSchemeWithInvalidFile() throws Exception
    {
        URL url = new File(System.getProperty("user.dir") + "/src/test-etc/i-do-not-exist").toURL();
        URLScriptLocator usl = new URLScriptLocator();
        InputStream in = usl.locate(url.toExternalForm());
        assertNull(in);
    }

    public void testSetHandlerOnDBI() throws Exception
    {
        DBI dbi = new DBI(Tools.CONN_STRING);
        dbi.setScriptLocator(new ClasspathScriptLocator());
    }

    public void testScriptWithCommentsAndNoExtensionClassPath() throws Exception
    {
        h.script("insert-script-with-comments");
        List r = h.query("all-something");
        assertEquals(3, r.size());
    }

    public void testDefaultDBIClassPathLoader() throws Exception
    {
        h.script("insert-script-with-comments.sql");
        List r = h.query("all-something");
        assertEquals(3, r.size());
    }

    public void testDefaultDBIFileSystemLoader() throws Exception
    {
        h.script("src/test-etc/insert-script-with-comments.sql");
        List r = h.query("all-something");
        assertEquals(3, r.size());
    }

    public void testDefaultWithUrl() throws Exception
    {
        URL url = new File(System.getProperty("user.dir") + "/src/test-etc/insert-script-with-comments.sql").toURL();
        h.script(url.toExternalForm());
        List r = h.query("all-something");
        assertEquals(3, r.size());
    }
}
