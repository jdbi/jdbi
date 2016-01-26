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
import java.sql.Statement;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestUpdateGeneratedKeys
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Before
    public void setUp() throws Exception
    {
        try (   final Connection conn = db.getConnectionFactory().openConnection();
                final Statement create = conn.createStatement())
        {
            create.execute("create table something_else ( id integer not null generated always as identity, name varchar(50) )");
        }
    }

    @Test
    public void testInsert() throws Exception
    {
        Handle h = db.openHandle();

        Update insert1 = h.createStatement("insert into something_else (name) values (:name)");
        insert1.bind("name", "Brian");
        Long id1 = insert1.executeAndReturnGeneratedKeys(long.class).findOnly();

        Assert.assertNotNull(id1);

        Update insert2 = h.createStatement("insert into something_else (name) values (:name)");
        insert2.bind("name", "Tom");
        Long id2 = insert2.executeAndReturnGeneratedKeys(long.class).findOnly();

        Assert.assertNotNull(id2);
        Assert.assertTrue(id2 > id1);
    }

    @Test
    public void testUpdate() throws Exception
    {
        Handle h = db.openHandle();

        Update insert = h.createStatement("insert into something_else (name) values (:name)");
        insert.bind("name", "Brian");
        Long id1 = insert.executeAndReturnGeneratedKeys(long.class).findOnly();

        Assert.assertNotNull(id1);

        Update update = h.createStatement("update something_else set name = :name where id = :id");
        update.bind("id", id1);
        update.bind("name", "Tom");
        Optional<Long> id2 = update.executeAndReturnGeneratedKeys(long.class).findFirst();

        Assert.assertFalse(id2.isPresent());
    }

    @Test
    public void testDelete() throws Exception
    {
        Handle h = db.openHandle();

        Update insert = h.createStatement("insert into something_else (name) values (:name)");
        insert.bind("name", "Brian");
        Long id1 = insert.executeAndReturnGeneratedKeys(long.class).findOnly();

        Assert.assertNotNull(id1);

        Update delete = h.createStatement("delete from something_else where id = :id");
        delete.bind("id", id1);
        Optional<Long> id2 = delete.executeAndReturnGeneratedKeys(long.class).findFirst();

        Assert.assertFalse(id2.isPresent());
    }
}
