package org.skife.jdbi.v2;

import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
class BlobArgument implements Argument
{
    private Blob value;

    BlobArgument(Blob value)
    {
        this.value = value;
    }

    public void apply(int position, PreparedStatement statement) throws SQLException
    {
        statement.setBlob(position, value);
    }
}
