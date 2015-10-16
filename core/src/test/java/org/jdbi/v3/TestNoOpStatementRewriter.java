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

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestNoOpStatementRewriter
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    private DBI dbi;

    @Before
    public void setUp() throws Exception
    {
        this.dbi = db.getDbi();
        dbi.setStatementRewriter(new NoOpStatementRewriter());
    }

    @Test
    public void testFoo() throws Exception
    {
        Handle h = dbi.open();
        h.insert("insert into something (id, name) values (1, 'Keith')");

        String name = h.createQuery("select name from something where id = ?")
                .bind(0, 1)
                .map(Something.class)
                .first()
                .getName();
        assertEquals("Keith", name);
    }

    @Test
    public void testBar() throws Exception
    {
        Handle h = dbi.open();
        h.insert("insert into something (id, name) values (1, 'Keith')");

        String name = h.createQuery("select name from something where id = ? and name = ?")
                .bind(0, 1)
                .bind(1, "Keith")
                .map(Something.class)
                .first().getName();
        assertEquals("Keith", name);
    }

    @Test
    public void testBaz() throws Exception
    {
        Handle h = dbi.open();
        h.insert("insert into something (id, name) values (1, 'Keith')");
    }
}
