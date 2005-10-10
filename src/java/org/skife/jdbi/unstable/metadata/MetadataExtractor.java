package org.skife.jdbi.unstable.metadata;

import java.sql.Connection;
import java.sql.SQLException;

public class MetadataExtractor
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
