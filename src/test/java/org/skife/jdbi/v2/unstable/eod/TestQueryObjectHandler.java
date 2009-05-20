/*
 * Copyright 2004-2007 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skife.jdbi.v2.unstable.eod;

import org.skife.jdbi.v2.DBITestCase;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;

import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class TestQueryObjectHandler extends DBITestCase
{
    private Handle handle;
    private MyQueries q;

    public void setUp() throws Exception
    {
        super.setUp();
        this.handle = openHandle();
        this.q = QueryObjectFactory.createQueryObject(MyQueries.class, handle.getConnection());
    }

    public void tearDown() throws Exception
    {
        q.close();
        super.tearDown();
    }

    public void testSimpleSelect() throws Exception
    {
        handle.insert("insert into something (id, name) values (?, ?)", 1, "Keith");
        List<Something> ds = q.getAllSomethings();

        assertNotNull(ds);
        Iterator<Something> i = ds.iterator();
        assertTrue(i.hasNext());
        Something s = i.next();
        assertEquals("Keith", s.getName());
    }

    public void testParameterizedSelect() throws Exception
    {
        handle.insert("insert into something (id, name) values (?, ?)", 1, "Keith");
        List<Something> ds = q.findByName("Keith");

        assertNotNull(ds);
        Iterator<Something> i = ds.iterator();
        assertTrue(i.hasNext());
        Something s = i.next();
        assertEquals("Keith", s.getName());
    }

    public void testIterator() throws Exception
    {
        handle.insert("insert into something (id, name) values (?, ?)", 1, "Keith");
        Iterator<Something> i = q.ittyAll();

        assertTrue(i.hasNext());
        Something s = i.next();
        assertEquals("Keith", s.getName());
        assertFalse(i.hasNext());
    }

    public void testSingle() throws Exception
    {
        handle.insert("insert into something (id, name) values (?, ?)", 1, "Keith");
        Something s = q.findById(1);
        assertEquals("Keith", s.getName());
    }

    public void testInsert() throws Exception
    {
        assertEquals(true, q.insert(1, "Keith"));
        Iterator<Something> as = q.ittyAll();
        assertTrue(as.hasNext());
        Something s = as.next();
        assertEquals("Keith", s.getName());
        assertFalse(as.hasNext());
    }

    public void testUpdate() throws Exception
    {
        q.insert(1, "Keith");
        q.updateNameById("Eric", 1);
        Something s = q.findById(1);
        assertNotNull(s);
        assertEquals("Eric", s.getName());
    }

    public void testDelete() throws Exception
    {
        q.insert(1, "Keith");
        q.deleteById(1);
        assertFalse(q.ittyAll().hasNext());
    }

    public void testBeanBinding() throws Exception
    {
        Something s = new Something(1, "Keith");
        q.insert(s);

        s = q.findById(1);
        assertNotNull(s);
        assertEquals("Keith", s.getName());

    }
}
