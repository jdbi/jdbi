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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.jdbi.v3.DBI;
import org.jdbi.v3.EmojiStatementRewriter;
import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.Handle;
import org.jdbi.v3.Something;
import org.jdbi.v3.sqlobject.customizers.RegisterMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestEmojiStatementRewriter
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        DBI dbi = db.getDbi();
        dbi.setStatementRewriter(new EmojiStatementRewriter());
        handle = dbi.open();
    }

    @Test
    public void testFoo() throws Exception
    {
        // test will raise exceptions if SQL is bogus -- if it uses the colon prefix form

        Wingding h = SqlObjectBuilder.attach(handle, Wingding.class);
        h.insert(1, "Joy");
        Something s = h.findById(1);
        assertThat(s.getName(), equalTo("Joy"));
    }


    @RegisterMapper(SomethingMapper.class)
    interface Wingding
    {
        @SqlUpdate("insert into something (id, name) values (😱, 😂)")
        void insert(@Bind("😱") int id, @Bind("😂") String name);

        @SqlQuery("select id, name from something where id = 💩")
        Something findById(@Bind("💩") int id);

    }

}
