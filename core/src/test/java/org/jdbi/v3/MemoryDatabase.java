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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.rules.ExternalResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class MemoryDatabase extends ExternalResource
{
    private final String uri = "jdbc:h2:mem:" + UUID.randomUUID();
    private Connection con;

    @Override
    protected void before() throws Throwable
    {
        con = DBI.open(uri).getConnection();
        try (Statement s = con.createStatement()) {
            s.execute("create table something ( id integer, name varchar(50), integerValue integer, intValue integer )");
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

    public String getConnectionString()
    {
        return uri;
    }

    public DBI getDbi()
    {
        return new DBI(uri);
    }

    public Handle openHandle()
    {
        return getDbi().open();
    }

    public DataSource getDataSource()
    {
        return new DriverManagerDataSource(getConnectionString());
    }
}
