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
package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.UnableToObtainConnectionException;
import org.skife.jdbi.v2.tweak.ConnectionFactory;
import org.skife.jdbi.v2.tweak.transactions.LocalTransactionHandler;

import javax.sql.DataSource;
import java.sql.SQLException;

public class DBI
{
    private final ConnectionFactory connectionFactory;

    public DBI(DataSource dataSource)
    {
        this(new DataSourceConnectionFactory(dataSource));
        assert(dataSource != null);
    }

    public DBI(ConnectionFactory connectionFactory)
    {
        assert(connectionFactory != null);
        this.connectionFactory = connectionFactory;
    }

    public Handle open()
    {
        try
        {
            return new BasicHandle(new LocalTransactionHandler(), connectionFactory.openConnection());
        }
        catch (SQLException e)
        {
            throw new UnableToObtainConnectionException(e);
        }
    }

    public static Handle open(DataSource dataSource)
    {
        return new DBI(dataSource).open();
    }
}
