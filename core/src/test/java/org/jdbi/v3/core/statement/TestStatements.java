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

import static org.assertj.core.api.Assertions.assertThat;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestStatements
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    private Handle h;

    @Before
    public void setUp() throws Exception
    {
        h = db.openHandle();
    }

    @After
    public void doTearDown() throws Exception
    {
        if (h != null) h.close();
    }

    @Test
    public void testStatement() throws Exception
    {
        int rows = h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        assertThat(rows).isEqualTo(1);
    }

    @Test
    public void testSimpleInsert() throws Exception
    {
        int c = h.insert("insert into something (id, name) values (1, 'eric')");
        assertThat(c).isEqualTo(1);
    }

    @Test
    public void testUpdate() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.createUpdate("update something set name = 'ERIC' where id = 1").execute();
        Something eric = h.createQuery("select * from something where id = 1").mapToBean(Something.class).list().get(0);
        assertThat(eric.getName()).isEqualTo("ERIC");
    }

    @Test
    public void testSimpleUpdate() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.update("update something set name = 'cire' where id = 1");
        Something eric = h.createQuery("select * from something where id = 1").mapToBean(Something.class).list().get(0);
        assertThat(eric.getName()).isEqualTo("cire");
    }
}
