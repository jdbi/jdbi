package org.skife.jdbi;

import junit.framework.TestCase;
import org.skife.jdbi.derby.Tools;
import org.skife.jdbi.DatabaseMetadata;

public class TestMetadata extends TestCase
{
    private Handle conn;
    private MetadataExtractor extractor;

    public void setUp() throws Exception
    {
        Tools.start();
        conn = DBI.open(Tools.CONN_STRING);
        Tools.dropAndCreateSomething();
        this.extractor = new MetadataExtractor(conn.getConnection());
    }

    public void tearDown() throws Exception
    {
        conn.close();
        Tools.stop();
    }

    public void testGetDBMetadataFromHandle() throws Exception
    {
        DatabaseMetadata m = conn.getDatabaseMetadata();
        assertNotNull(m);
    }

    public void testGetTableNames() throws Exception
    {
        Table[] table_arr = extractor.buildDatabaseMetadata().getTableMetadata();
        boolean found_a = false;
        for (int i = 0; i < table_arr.length; i++)
        {
            Table m = table_arr[i];
            String name = m.getName();
            found_a = found_a || name.toUpperCase().equals("SOMETHING");
        }
        assertTrue(found_a);
    }

    public void testGetSpecificTable() throws Exception
    {
        Table table = extractor.buildDatabaseMetadata().getTableNamed("SOMETHING");
        assertNotNull(table);
    }

    public void testGetSchemaNameFromTable() throws Exception
    {
        Table table = extractor.buildDatabaseMetadata().getTableNamed("SOMETHING");
        assertEquals("APP", table.getSchemaName());
    }

    public void testGetCatalogFromTable() throws Exception
    {
        Table table = extractor.buildDatabaseMetadata().getTableNamed("SOMETHING");
        assertEquals("", table.getCatalogName());
    }

    public void testGetSchemas() throws Exception
    {
        Schema[] schemi = extractor.buildDatabaseMetadata().getSchema();
        boolean found = false;
        for (int i = 0; i < schemi.length; i++)
        {
            Schema schema = schemi[i];
            found = found || schema.getName().equals("APP");
        }
        assertTrue(found);
    }

    public void testGetSchemaNamed() throws Exception
    {
        Schema app = extractor.buildDatabaseMetadata().getSchemaNamed("APP");
        assertEquals("APP", app.getName());
    }

    public void testGetTablesFromSchemaContainsApp() throws Exception
    {
        Schema app = extractor.buildDatabaseMetadata().getSchemaNamed("APP");
        Table[] tables = app.getTables();
        boolean found = false;
        for (int i = 0; i < tables.length; i++)
        {
            Table table = tables[i];
            found = found || table.getName().toUpperCase().equals("SOMETHING");
        }
        assertEquals(1, tables.length);
        assertTrue(found);
    }

    public void testGetTablesFromSchemaContainsOnlyApp() throws Exception
    {
        Schema app = extractor.buildDatabaseMetadata().getSchemaNamed("APP");
        Table[] tables = app.getTables();
        assertEquals(1, tables.length);
    }
}
