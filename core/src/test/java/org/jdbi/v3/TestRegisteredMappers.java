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

import static org.junit.Assert.assertEquals;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.jdbi.v3.tweak.ResultSetMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class TestRegisteredMappers
{
    private DBI dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        dbi = new DBI("jdbc:h2:mem:" + UUID.randomUUID());
        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");

    }

    @After
    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
    }

    @Test
    public void testRegisterInferredOnDBI() throws Exception
    {
        dbi.registerMapper(new SomethingMapper());
        Something sam = dbi.withHandle(handle1 -> {
            handle1.insert("insert into something (id, name) values (18, 'Sam')");

            return handle1.createQuery("select id, name from something where id = :id")
                .bind("id", 18)
                .mapTo(Something.class)
                .findOnly();
        });

        assertEquals("Sam", sam.getName());
    }
}

class SomethingMapper implements ResultSetMapper<Something>
{
    @Override
    public Something map(int index, ResultSet r, StatementContext ctx) throws SQLException
    {
        return new Something(r.getInt("id"), r.getString("name"));
    }
}
