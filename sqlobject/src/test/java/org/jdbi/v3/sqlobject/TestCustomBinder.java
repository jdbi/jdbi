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

import org.jdbi.v3.MemoryDatabase;
import org.jdbi.v3.Something;
import org.jdbi.v3.sqlobject.customizers.Mapper;
import org.jdbi.v3.sqlobject.mixins.CloseMe;
import org.junit.Rule;
import org.junit.Test;

public class TestCustomBinder
{
    @Rule
    public MemoryDatabase db = new MemoryDatabase();

    @Test
    public void testFoo() throws Exception
    {
        db.getSharedHandle().execute("insert into something (id, name) values (2, 'Martin')");
        try (Spiffy spiffy = SqlObjectBuilder.open(db.getDbi(), Spiffy.class)) {
            Something s = spiffy.findSame(new Something(2, "Unknown"));
            assertEquals("Martin", s.getName());
        }
    }

    @Test
    public void testCustomBindingAnnotation() throws Exception
    {
        Spiffy s = SqlObjectBuilder.attach(db.getSharedHandle(), Spiffy.class);

        s.insert(new Something(2, "Keith"));

        assertEquals("Keith", s.findNameById(2));
    }

    public static interface Spiffy extends CloseMe
    {
        @SqlQuery("select id, name from something where id = :it.id")
        @Mapper(SomethingMapper.class)
        public Something findSame(@Bind(value = "it", binder = SomethingBinderAgainstBind.class) Something something);

        @SqlUpdate("insert into something (id, name) values (:s.id, :s.name)")
        public int insert(@BindSomething("s") Something something);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind("id") int i);
    }
}
