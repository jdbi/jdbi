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
package org.jdbi.v3.core;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Optional;

import org.jdbi.v3.core.statement.Update;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

        Update insert1 = h.createUpdate("insert into something_else (name) values (:name)");
        insert1.bind("name", "Brian");
        Long id1 = insert1.executeAndReturnGeneratedKeys().mapTo(long.class).findOnly();

        assertThat(id1).isNotNull();

        Update insert2 = h.createUpdate("insert into something_else (name) values (:name)");
        insert2.bind("name", "Tom");
        Long id2 = insert2.executeAndReturnGeneratedKeys().mapTo(long.class).findOnly();

        assertThat(id2).isNotNull();
        assertThat(id2).isGreaterThan(id1);
    }

    @Test
    public void testUpdate() throws Exception
    {
        Handle h = db.openHandle();

        Update insert = h.createUpdate("insert into something_else (name) values (:name)");
        insert.bind("name", "Brian");
        Long id1 = insert.executeAndReturnGeneratedKeys().mapTo(long.class).findOnly();

        assertThat(id1).isNotNull();

        Update update = h.createUpdate("update something_else set name = :name where id = :id");
        update.bind("id", id1);
        update.bind("name", "Tom");
        Optional<Long> id2 = update.executeAndReturnGeneratedKeys().mapTo(long.class).findFirst();

        assertThat(id2.isPresent()).isFalse();
    }

    @Test
    public void testDelete() throws Exception
    {
        Handle h = db.openHandle();

        Update insert = h.createUpdate("insert into something_else (name) values (:name)");
        insert.bind("name", "Brian");
        Long id1 = insert.executeAndReturnGeneratedKeys().mapTo(long.class).findOnly();

        assertThat(id1).isNotNull();

        Update delete = h.createUpdate("delete from something_else where id = :id");
        delete.bind("id", id1);
        Optional<Long> id2 = delete.executeAndReturnGeneratedKeys().mapTo(long.class).findFirst();

        assertThat(id2.isPresent()).isFalse();
    }
}
