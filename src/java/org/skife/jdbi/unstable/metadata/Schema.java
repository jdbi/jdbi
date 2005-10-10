package org.skife.jdbi.unstable.metadata;

import org.skife.jdbi.unstable.metadata.Table;
import org.skife.jdbi.unstable.metadata.DatabaseMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Schema
{
    private String name;
    private String catalog;
    private DatabaseMetadata meta;

    Schema(DatabaseMetadata meta, ResultSet results)
    {
        this.meta = meta;
        try
        {
            name = results.getString("TABLE_SCHEM");
        }
        catch (SQLException e)
        {}
        try
        {
            catalog = results.getString("TABLE_CATALOG");
        }
        catch (SQLException e)
        {}

    }

    public String getName()
    {
        return name;
    }

    public String getCatalog()
    {
        return catalog;
    }

    public Table[] getTables()
    {
        return meta.getTablesForSchemaNamed(this.getName());    
    }
}
