/*
 * Copyright (C) 2004 - 2013 Brian McCallister
 *
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
package org.skife.jdbi.derby;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.skife.jdbi.HandyMapThing;

public class DerbyHelper
{
    public static final String DERBY_SYSTEM_HOME = "target/test-db";

    private Driver driver;
    private boolean running = false;
    private EmbeddedDataSource dataSource;

    private final String dbName;

    public DerbyHelper()
    {
        this.dbName = "testing"; //  + UUID.randomUUID().toString();
    }

    public void start() throws SQLException, IOException
    {
        if (!running)
        {
            running = true;
            System.setProperty("derby.system.home", DERBY_SYSTEM_HOME);
            File db = new File("target/test-db");
            db.mkdirs();

            dataSource = new EmbeddedDataSource();
            dataSource.setCreateDatabase("create");
            dataSource.setDatabaseName(dbName);

            final Connection conn = dataSource.getConnection();
            conn.close();
        }
    }

    public void stop() throws SQLException
    {
        final Connection conn = getConnection();
        final Statement delete = conn.createStatement();
        try
        {
            delete.execute("delete from something");
        }
        catch (SQLException e)
        {
            // may not exist
        }
        delete.close();
        final String[] drops = {"drop table something",
                                "drop function do_it",
                                "drop procedure INSERTSOMETHING"};
        for (String drop : drops)
        {
            final Statement stmt = conn.createStatement();
            try
            {
                stmt.execute(drop);
            }
            catch (Exception e)
            {
                // may not exist
            }

        }
    }

    public Connection getConnection() throws SQLException
    {
        return dataSource.getConnection();
    }

    public String getDbName()
    {
        return dbName;
    }

    public String getJdbcConnectionString()
    {
        return "jdbc:derby:" + getDbName();
    }

    public void dropAndCreateSomething() throws SQLException
    {
        final Connection conn = getConnection();

        final Statement create = conn.createStatement();
        try
        {
            create.execute("create table something ( id integer, name varchar(50), integerValue integer, intValue integer )");
        }
        catch (Exception e)
        {
            // probably still exists because of previous failed test, just delete then
            create.execute("delete from something");
        }
        create.close();
        conn.close();
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public static String doIt()
    {
        return "it";
    }

    public static <K> HandyMapThing<K> map(K k, Object v)
    {
        HandyMapThing<K>s =  new HandyMapThing<K>();
        return s.add(k, v);
    }
}
