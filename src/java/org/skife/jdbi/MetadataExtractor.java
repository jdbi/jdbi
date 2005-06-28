package org.skife.jdbi;

import java.sql.Connection;
import java.sql.SQLException;

class MetadataExtractor
{
    private Connection conn;

    MetadataExtractor(Connection conn)
    {
        this.conn = conn;
    }

    DatabaseMetadata buildDatabaseMetadata()
    {
        return (DatabaseMetadata) Helper.sql(new Helper() {
            Object fetch() throws SQLException
            {
                return new DatabaseMetadata(conn.getMetaData());
            }
        });
    }
}
