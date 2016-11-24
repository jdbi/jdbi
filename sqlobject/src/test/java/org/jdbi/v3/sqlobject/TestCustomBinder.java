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

import static org.assertj.core.api.Assertions.assertThat;

import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.customizers.UseRowMapper;
import org.junit.Rule;
import org.junit.Test;

public class TestCustomBinder
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    @Test
    public void testFoo() throws Exception
    {
        db.getSharedHandle().execute("insert into something (id, name) values (2, 'Martin')");
        db.getJdbi().useExtension(Spiffy.class, spiffy -> {
            Something s = spiffy.findSame(new Something(2, "Unknown"));
            assertThat(s.getName()).isEqualTo("Martin");
        });
    }

    @Test
    public void testCustomBindingAnnotation() throws Exception
    {
        Spiffy s = db.getSharedHandle().attach(Spiffy.class);

        s.insert(new Something(2, "Keith"));

        assertThat(s.findNameById(2)).isEqualTo("Keith");
    }

    public interface Spiffy
    {
        @SqlQuery("select id, name from something where id = :it.id")
        @UseRowMapper(SomethingMapper.class)
        Something findSame(@BindSomething("it") Something something);

        @SqlUpdate("insert into something (id, name) values (:s.id, :s.name)")
        int insert(@BindSomething("s") Something something);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind("id") int i);
    }
}
