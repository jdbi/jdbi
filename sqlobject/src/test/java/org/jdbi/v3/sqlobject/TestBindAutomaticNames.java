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
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.customizers.RegisterRowMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestBindAutomaticNames
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        handle = db.getSharedHandle();
        handle.execute("insert into something (id, name) values (7, 'Tim')");
    }

    @Test
    public void testAnnotationNoValue() throws Exception
    {
        Spiffy spiffy = handle.attach(Spiffy.class);
        Something s = spiffy.findById(7);
        assertThat(s.getName()).isEqualTo("Tim");
    }

    @Test
    public void testNoAnnotation() throws Exception
    {
        Spiffy spiffy = db.getSharedHandle().attach(Spiffy.class);
        Something s = spiffy.findByIdNoAnnotation(7);
        assertThat(s.getName()).isEqualTo("Tim");
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface Spiffy
    {
        @SqlQuery("select id, name from something where id = :id")
        Something findById(@Bind int id);

        @SqlQuery("select id, name from something where id = :id")
        Something findByIdNoAnnotation(int id);
    }
}
