package org.skife.jdbi.unstable.metadata;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Table
{
    private static String NAME = "TABLE_NAME";
    private static String SCHEMA = "TABLE_SCHEM";
    private static String CATALOG = "TABLE_CAT";
    private String name;
    private String schema;
    private String catalog;

    /**
     * Pulls info from current row only
     * @param results
     */
    Table(ResultSet results) throws SQLException
    {
        this.name = results.getString(NAME);
        this.schema = results.getString(SCHEMA);
        this.catalog = results.getString(CATALOG);
    }

    public String getName()
    {
        return name;
    }

    public String getSchemaName()
    {
        return schema;
    }

    public String getCatalogName()
    {
        return catalog;
    }
}
