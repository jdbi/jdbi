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
import org.jdbi.v3.Something;
import org.jdbi.v3.sqlobject.customizers.Mapper;
import org.jdbi.v3.sqlobject.mixins.CloseMe;
import org.junit.Rule;
import org.junit.Test;

@SuppressWarnings("resource")
public class TestDefaultMethods
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Test
    public void testDefaultMethod() throws Exception {
        Spiffy dao = SqlObjectBuilder.onDemand(db.getDbi(), Spiffy.class);
        Something test = dao.insertAndReturn(3, "test");
        assertEquals(3, test.getId());
        assertEquals("test", test.getName());
    }

    @Test
    public void testOverride() throws Exception {
        SpiffyOverride dao = SqlObjectBuilder.onDemand(db.getDbi(), SpiffyOverride.class);
        assertEquals(null, dao.insertAndReturn(123, "fake"));
    }

    @Test
    public void testOverrideWithDefault() throws Exception {
        SpiffyOverrideWithDefault dao = SqlObjectBuilder.onDemand(db.getDbi(), SpiffyOverrideWithDefault.class);
        assertEquals(-6, dao.insertAndReturn(123, "fake").getId());
    }

    public interface Spiffy extends CloseMe
    {
        @SqlQuery("select id, name from something where id = :id")
        @Mapper(SomethingMapper.class)
        Something byId(@Bind("id") int id);

        @SqlUpdate("insert into something (id, name) values (:it.id, :it.name)")
        void insert(@Bind(value = "it", binder = SomethingBinderAgainstBind.class) Something it);

        default Something insertAndReturn(int id, String name) {
            insert(new Something(id, name));
            return byId(id);
        }
    }

    public interface SpiffyOverride extends Spiffy
    {
        @Override
        @SqlQuery("select id, name from something where id = :id")
        Something insertAndReturn(@Bind int id, @Bind String name);
    }

    public interface SpiffyOverrideWithDefault extends SpiffyOverride
    {
        @Override
        default Something insertAndReturn(int id, String name) {
            return new Something(-6, "what");
        }
    }
}
