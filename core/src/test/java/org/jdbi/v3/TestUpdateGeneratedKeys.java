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
package org.jdbi.v3;

import java.sql.Connection;
import java.sql.Statement;

import org.jdbi.derby.Tools;
import org.jdbi.v3.util.LongMapper;

public class TestUpdateGeneratedKeys extends DBITestCase
{
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        final Connection conn = Tools.getConnection();

        final Statement create = conn.createStatement();
        try
        {
            create.execute("create table something_else ( id integer not null generated always as identity, name varchar(50) )");
        }
        catch (Exception e)
        {
            // probably still exists because of previous failed test, just delete then
            create.execute("delete from something_else");
        }
        create.close();
        conn.close();
    }

    public void testInsert() throws Exception
    {
        Handle h = openHandle();

        Update insert1 = h.createStatement("insert into something_else (name) values (:name)");
        insert1.bind("name", "Brian");
        Long id1 = insert1.executeAndReturnGeneratedKeys(LongMapper.FIRST).first();

        assertNotNull(id1);

        Update insert2 = h.createStatement("insert into something_else (name) values (:name)");
        insert2.bind("name", "Tom");
        Long id2 = insert2.executeAndReturnGeneratedKeys(LongMapper.FIRST).first();

        assertNotNull(id2);
        assertTrue(id2 > id1);
    }

    public void testUpdate() throws Exception
    {
        Handle h = openHandle();

        Update insert = h.createStatement("insert into something_else (name) values (:name)");
        insert.bind("name", "Brian");
        Long id1 = insert.executeAndReturnGeneratedKeys(LongMapper.FIRST).first();

        assertNotNull(id1);

        Update update = h.createStatement("update something_else set name = :name where id = :id");
        update.bind("id", id1);
        update.bind("name", "Tom");
        Long id2 = update.executeAndReturnGeneratedKeys(LongMapper.FIRST).first();

        assertNull(id2);
    }

    public void testDelete() throws Exception
    {
        Handle h = openHandle();

        Update insert = h.createStatement("insert into something_else (name) values (:name)");
        insert.bind("name", "Brian");
        Long id1 = insert.executeAndReturnGeneratedKeys(LongMapper.FIRST).first();

        assertNotNull(id1);

        Update delete = h.createStatement("delete from something_else where id = :id");
        delete.bind("id", id1);
        Long id2 = delete.executeAndReturnGeneratedKeys(LongMapper.FIRST).first();

        assertNull(id2);
    }
}
