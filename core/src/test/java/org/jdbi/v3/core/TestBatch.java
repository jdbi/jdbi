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

import java.util.List;

import org.junit.Rule;
import org.junit.Test;

public class TestBatch
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Test
    public void testBasics() throws Exception
    {
        Handle h = db.openHandle();

        Batch b = h.createBatch();
        b.add("insert into something (id, name) values (0, 'Keith')");
        b.add("insert into something (id, name) values (1, 'Eric')");
        b.add("insert into something (id, name) values (2, 'Brian')");
        b.execute();

        List<Something> r = h.createQuery("select * from something order by id").mapToBean(Something.class).list();
        assertThat(r).hasSize(3);
    }
}
