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
package org.skife.jdbi;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Collection;

class QueueingBatch implements Batch
{
    private final List statements = new ArrayList();
    private final Connection conn;

    public QueueingBatch(Connection conn)
    {
        this.conn = conn;
    }

    public Batch add(String statement)
    {
        statements.add(statement);
        return this;
    }

    public Batch addAll(Collection statements)
    {
        for (Iterator iterator = statements.iterator(); iterator.hasNext();)
        {
            final Object o = iterator.next();
            statements.add(String.valueOf(o));
        }
        return null;
    }

    public int[] execute() throws DBIException
    {
        final Iterator itty = statements.iterator();
        final Statement stmt;
        try
        {
            stmt = conn.createStatement();
            while (itty.hasNext())
            {
                stmt.addBatch((String) itty.next());
            }
            int[] results = stmt.executeBatch();
            stmt.close();
            return results;
        }
        catch (SQLException e)
        {
            throw new DBIException(e.getMessage(), e);
        }
        finally
        {
            statements.clear();
        }
    }
}
