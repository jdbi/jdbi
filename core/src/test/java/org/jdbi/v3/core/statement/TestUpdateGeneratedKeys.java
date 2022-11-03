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
package org.jdbi.v3.core.statement;

import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestUpdateGeneratedKeys {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    @BeforeEach
    public void setUp() throws Exception {
        h2Extension.getSharedHandle().execute("create table something_else (id integer not null generated always as identity, name varchar(50))");
    }

    @Test
    public void testInsert() {
        Handle h = h2Extension.getSharedHandle();

        Update insert1 = h.createUpdate("insert into something_else (name) values (:name)");
        insert1.bind("name", "Brian");
        Long id1 = insert1.executeAndReturnGeneratedKeys().mapTo(long.class).one();

        assertThat(id1).isNotNull();

        Update insert2 = h.createUpdate("insert into something_else (name) values (:name)");
        insert2.bind("name", "Tom");
        Long id2 = insert2.executeAndReturnGeneratedKeys().mapTo(long.class).one();

        assertThat(id2).isNotNull()
                .isGreaterThan(id1);
    }

    @Test
    public void testUpdate() {
        Handle h = h2Extension.getSharedHandle();

        Update insert = h.createUpdate("insert into something_else (name) values (:name)");
        insert.bind("name", "Brian");
        Long id1 = insert.executeAndReturnGeneratedKeys().mapTo(long.class).one();

        assertThat(id1).isNotNull();

        Update update = h.createUpdate("update something_else set name = :name where id = :id");
        update.bind("id", id1);
        update.bind("name", "Tom");
        Optional<Long> id2 = update.executeAndReturnGeneratedKeys().mapTo(long.class).findFirst();

        assertThat(id2).hasValue(id1);
    }

    @Test
    public void testDelete() {
        Handle h = h2Extension.getSharedHandle();

        Update insert = h.createUpdate("insert into something_else (name) values (:name)");
        insert.bind("name", "Brian");
        Long id1 = insert.executeAndReturnGeneratedKeys().mapTo(long.class).one();

        assertThat(id1).isNotNull();

        Update delete = h.createUpdate("delete from something_else where id = :id");
        delete.bind("id", id1);
        Optional<Long> id2 = delete.executeAndReturnGeneratedKeys().mapTo(long.class).findFirst();

        assertThat(id2).isNotPresent();
    }
}
