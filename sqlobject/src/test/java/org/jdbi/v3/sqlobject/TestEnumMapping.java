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

import static org.junit.Assert.assertSame;

import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.customizers.RegisterRowMapper;
import org.junit.Rule;
import org.junit.Test;

public class TestEnumMapping
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    @Test
    public void testEnums() throws Exception
    {
        Spiffy spiffy = db.getSharedHandle().attach(Spiffy.class);

        int bobId = spiffy.addCoolName(CoolName.BOB);
        int joeId = spiffy.addCoolName(CoolName.JOE);

        assertSame(CoolName.BOB, spiffy.findById(bobId));
        assertSame(CoolName.JOE, spiffy.findById(joeId));
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface Spiffy
    {
        @SqlUpdate("insert into something(name) values(:name)")
        @GetGeneratedKeys
        int addCoolName(@Bind("name") CoolName coolName);

        @SqlQuery("select name from something where id = :id")
        CoolName findById(@Bind("id") int id);
    }

    public enum CoolName
    {
        BOB, FRANK, JOE
    }
}
