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

import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.Handle;
import org.jdbi.v3.Something;
import org.jdbi.v3.sqlobject.customizers.RegisterMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestClasspathStatementLocator {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @Before
    public void setUp() throws Exception {
        handle = db.getSharedHandle();
    }

    @Test
    public void testBam() throws Exception {
        handle.execute("insert into something (id, name) values (6, 'Martin')");

        Something s = handle.attach(Cromulence.class).findById(6L);
        assertThat(s.getName(), equalTo("Martin"));
    }

    @RegisterMapper(SomethingMapper.class)
    interface Cromulence {
        @SqlQuery
        Something findById(@Bind("id") Long id);
    }
}
