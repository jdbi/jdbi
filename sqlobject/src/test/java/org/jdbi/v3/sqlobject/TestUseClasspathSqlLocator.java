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

import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.customizers.RegisterRowMapper;
import org.jdbi.v3.sqlobject.locator.UseClasspathSqlLocator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestUseClasspathSqlLocator {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @Before
    public void setUp() throws Exception {
        handle = db.getSharedHandle();
        handle.execute("insert into something (id, name) values (6, 'Martin')");
    }

    @Test
    public void testBam() throws Exception {
        Something s = handle.attach(Cromulence.class).findById(6L);
        assertThat(s.getName(), equalTo("Martin"));
    }

    @Test
    public void testOverride() throws Exception {
        Something s = handle.attach(SubCromulence.class).findById(6L);
        assertThat(s.getName(), equalTo("overridden"));
    }

    @Test
    public void testCachedOverride() throws Exception {
        Something s = handle.attach(Cromulence.class).findById(6L);
        assertThat(s.getName(), equalTo("Martin"));

        // and now make sure we don't accidentally cache the statement from above
        s = handle.attach(SubCromulence.class).findById(6L);
        assertThat(s.getName(), equalTo("overridden"));
    }

    @UseClasspathSqlLocator
    @RegisterRowMapper(SomethingMapper.class)
    interface Cromulence {
        @SqlQuery
        Something findById(@Bind("id") Long id);
    }

    @RegisterRowMapper(SomethingMapper.class)
    @UseClasspathSqlLocator
    static interface SubCromulence extends Cromulence { }
}
