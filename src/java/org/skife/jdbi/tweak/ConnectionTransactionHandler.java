/* Copyright 2004-2005 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.tweak;

import org.skife.jdbi.DBIException;
import org.skife.jdbi.Handle;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Provide explicit local transaction management directly on the the JDBC <code>Connection</code>
 * <p />
 * This is the default transaction handler.
 */
public class ConnectionTransactionHandler implements TransactionHandler
{

    /**
     * Called when a transaction is started
     */
    public void begin(Handle handle)
    {
        final Connection conn = handle.getConnection();
        try
        {
            conn.setAutoCommit(false);
        }
        catch (SQLException e)
        {
            throw new DBIException("unable to begin a transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Called when a transaction is committed
     */
    public void commit(Handle handle)
    {
        final Connection conn = handle.getConnection();
        try
        {
            conn.commit();
            conn.setAutoCommit(true);
        }
        catch (SQLException e)
        {
            throw new DBIException("unable to commit transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Called when a transaction is rolled back
     */
    public void rollback(Handle handle)
    {
        final Connection conn = handle.getConnection();
        try
        {
            conn.rollback();
            conn.setAutoCommit(true);
        }
        catch (SQLException e)
        {
            throw new DBIException("unable to roll back transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Called to test if a handle is in a transaction
     */
    public boolean isInTransaction(Handle handle)
    {
        final Connection conn = handle.getConnection();
        try
        {
            return !conn.getAutoCommit();
        }
        catch (SQLException e)
        {
            throw new DBIException(e.getMessage(), e);
        }
    }
}
