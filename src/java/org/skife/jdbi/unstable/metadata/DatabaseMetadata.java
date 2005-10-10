package org.skife.jdbi.unstable.metadata;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseMetadata
{
    private DatabaseMetaData data;

    DatabaseMetadata(DatabaseMetaData data)
    {
        this.data = data;
    }

    public Table[] getTableMetadata()
    {
        List results = (List) Helper.sql(new Helper() {
            public Object fetch() throws SQLException
            {
                ResultSet results = data.getTables(null, null, null, null);
                List tables = new ArrayList();
                while (results.next())
                {
                    tables.add(new Table(results));
                }
                results.close();
                return tables;
            }
        });
        return (Table[]) results.toArray(new Table[results.size()]);
    }

    /**
     *
     * @param name the case-insensitive table name
     * @return table metadata for the first table encountered with that name
     * @todo add ability to constrain by schema, etc
     */
    public Table getTableNamed(String name)
    {
        for (int i = 0; i < getTableMetadata().length; i++)
        {
            Table metadata = getTableMetadata()[i];
            if (metadata.getName().toUpperCase().equals(name.toUpperCase()))
            {
                return metadata;
            }
        }
        return null;
    }

    /**
     * Obtain an array of all the defined schema
     */
    public Schema[] getSchema()
    {
        List results = (List) Helper.sql(new Helper() {
            public Object fetch() throws SQLException
            {
                ResultSet results = data.getSchemas();
                List tables = new ArrayList();
                while (results.next())
                {
                    tables.add(new Schema(DatabaseMetadata.this, results));
                }
                results.close();
                return tables;
            }
        });
        return (Schema[]) results.toArray(new Schema[results.size()]);
    }

    public Schema getSchemaNamed(String name)
    {
        for (int i = 0; i < getSchema().length; i++)
        {
            Schema schema = getSchema()[i];
            if (schema.getName().toUpperCase().equals(name.toUpperCase())) return schema;
        }
        return null;
    }

    public Table[] getTablesForSchemaNamed(final String name)
    {
        List results = (List) Helper.sql(new Helper() {
            public Object fetch() throws SQLException
            {
                ResultSet results = data.getTables(null, name, null, null);
                List tables = new ArrayList();
                while (results.next())
                {
                    tables.add(new Table(results));
                }
                results.close();
                return tables;
            }
        });
        return (Table[]) results.toArray(new Table[results.size()]);
    }
}
