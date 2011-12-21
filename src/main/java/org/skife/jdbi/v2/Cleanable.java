package org.skife.jdbi.v2;

import java.sql.SQLException;

public interface Cleanable
{
    void cleanup() throws SQLException;
}
