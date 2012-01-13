package org.skife.jdbi.v2;

import org.skife.jdbi.derby.Tools;
import org.skife.jdbi.v2.util.LongMapper;

import java.sql.Connection;
import java.sql.Statement;

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
