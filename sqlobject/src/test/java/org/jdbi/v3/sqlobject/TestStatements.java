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
package org.jdbi.v3.sqlobject;

import static org.junit.Assert.assertEquals;

import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.sqlobject.mixins.CloseMe;
import org.junit.Rule;
import org.junit.Test;

public class TestStatements
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Test
    public void testInsert() throws Exception
    {
        try (Inserter i = SqlObjectBuilder.open(db.getDbi(), Inserter.class)) {
            // this is what is under test here
            int rows_affected = i.insert(2, "Diego");

            String name = db.getSharedHandle().createQuery("select name from something where id = 2").mapTo(String.class).findOnly();

            assertEquals(1, rows_affected);
            assertEquals("Diego", name);
        }
    }

    @Test
    public void testInsertWithVoidReturn() throws Exception
    {
        try (Inserter i = SqlObjectBuilder.open(db.getDbi(), Inserter.class)) {
            // this is what is under test here
            i.insertWithVoidReturn(2, "Diego");

            String name = db.getSharedHandle().createQuery("select name from something where id = 2").mapTo(String.class).findOnly();

            assertEquals("Diego", name);
        }
    }

    public interface Inserter extends CloseMe
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        int insert(@Bind("id") long id, @Bind("name") String name);

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insertWithVoidReturn(@Bind("id") long id, @Bind("name") String name);
    }
}
