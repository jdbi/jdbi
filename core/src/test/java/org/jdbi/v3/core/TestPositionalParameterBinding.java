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

import static org.assertj.core.api.Assertions.assertThat;

import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestPositionalParameterBinding
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    private Handle h;

    @Before
    public void setUp() throws Exception
    {
        h = db.openHandle();
    }

    @Test
    public void testSetPositionalString() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");

        Something eric = h.createQuery("select * from something where name = ?")
                .bind(0, "eric")
                .mapToBean(Something.class)
                .list()
                .get(0);
        assertThat(eric.getId()).isEqualTo(1);
    }

    @Test
    public void testSetPositionalInteger() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");

        Something eric = h.createQuery("select * from something where id = ?")
                .bind(0, 1)
                .mapToBean(Something.class)
                .list().get(0);
        assertThat(eric.getId()).isEqualTo(1);
    }

    @Test(expected = UnableToExecuteStatementException.class)
    public void testBehaviorOnBadBinding1() throws Exception
    {
        h.createQuery("select * from something where id = ? and name = ?")
                .bind(0, 1)
                .mapToBean(Something.class)
                .list();
    }

    @Test(expected = UnableToExecuteStatementException.class)
    public void testBehaviorOnBadBinding2() throws Exception
    {
        h.createQuery("select * from something where id = ?")
                .bind(1, 1)
                .bind(2, "Hi")
                .mapToBean(Something.class)
                .list();
    }

    @Test
    public void testInsertParamBinding() throws Exception
    {
        int count = h.createUpdate("insert into something (id, name) values (?, 'eric')")
                .bind(0, 1)
                .execute();

        assertThat(count).isEqualTo(1);
    }

    @Test
    public void testPositionalConvenienceInsert() throws Exception
    {
        int count = h.insert("insert into something (id, name) values (?, ?)", 1, "eric");

        assertThat(count).isEqualTo(1);
    }
}
