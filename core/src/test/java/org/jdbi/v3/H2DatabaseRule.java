/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.jdbi.v3.tweak.ConnectionFactory;
import org.junit.rules.ExternalResource;

public class H2DatabaseRule extends ExternalResource
{
    private final String uri = "jdbc:h2:mem:" + UUID.randomUUID();
    private Connection con;
    private DBI dbi;
    private Handle sharedHandle;
    private boolean installPlugins = false;

    @Override
    protected void before() throws Throwable
    {
        dbi = DBI.create(uri);
        if (installPlugins) {
            dbi.installPlugins();
        }
        sharedHandle = dbi.open();
        con = sharedHandle.getConnection();
        try (Statement s = con.createStatement()) {
            s.execute("create table something ( id identity primary key, name varchar(50), integerValue integer, intValue integer )");
        }
    }

    @Override
    protected void after()
    {
        try {
            con.close();
        } catch (SQLException e) {
            throw new AssertionError(e);
        }
    }

    public H2DatabaseRule withPlugins()
    {
        installPlugins = true;
        return this;
    }

    public String getConnectionString()
    {
        return uri;
    }

    public DBI getDbi()
    {
        return dbi;
    }

    public Handle getSharedHandle()
    {
        return sharedHandle;
    }

    public Handle openHandle()
    {
        return getDbi().open();
    }

    public ConnectionFactory getConnectionFactory()
    {
        return () -> DriverManager.getConnection(getConnectionString());
    }
}
