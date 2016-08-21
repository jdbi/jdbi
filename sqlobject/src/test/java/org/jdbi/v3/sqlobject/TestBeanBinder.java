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

import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.sqlobject.customizers.RegisterRowMapper;
import org.jdbi.v3.sqlobject.subpackage.PrivateImplementationFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestBeanBinder
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        handle = db.getSharedHandle();
    }

    @Test
    public void testInsert() throws Exception
    {
        Spiffy s = handle.attach(Spiffy.class);
        s.insert(new Something(2, "Bean"));

        String name = handle.createQuery("select name from something where id = 2").mapTo(String.class).findOnly();
        assertEquals("Bean", name);
    }

    @Test
    public void testRead() throws Exception
    {
        Spiffy s = handle.attach(Spiffy.class);
        handle.insert("insert into something (id, name) values (17, 'Phil')");
        Something phil = s.findByEqualsOnBothFields(new Something(17, "Phil"));
        assertEquals("Phil", phil.getName());
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface Spiffy {

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        int insert(@BindBean Something s);

        @SqlQuery("select id, name from something where id = :s.id and name = :s.name")
        Something findByEqualsOnBothFields(@BindBean("s") Something s);

        @SqlQuery("select :pi.value")
        String selectPublicInterfaceValue(@BindBean("pi") PublicInterface pi);
    }

    @Test
    public void testBindingPrivateTypeUsingPublicInterface() throws Exception
    {
        Spiffy s = handle.attach(Spiffy.class);
        assertEquals("IShouldBind", s.selectPublicInterfaceValue(PrivateImplementationFactory.create()));
    }

    public interface PublicInterface {
        String getValue();
    }
}
