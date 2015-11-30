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

import com.fasterxml.classmate.GenericType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class TestOptional {
    private static final String SELECT_BY_NAME = "select * from something " +
            "where :name is null or name = :name " +
            "order by id";

    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    Handle handle;

    @Before
    public void createTestData() {
        handle = db.openHandle();
        handle.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        handle.createStatement("insert into something (id, name) values (2, 'brian')").execute();
    }

    @Test
    public void testDynamicBindOptionalPresent() throws Exception {
        Something result = handle.createQuery(SELECT_BY_NAME)
                .dynamicBind(new GenericType<Optional<String>>() {}, "name", Optional.of("eric"))
                .mapToBean(Something.class)
                .findOnly();

        assertEquals(1, result.getId());
        assertEquals("eric", result.getName());
    }

    @Test
    public void testDynamicBindOptionalEmpty() throws Exception {
        List<Something> result = handle.createQuery(SELECT_BY_NAME)
                .dynamicBind(new GenericType<Optional<String>>() {}, "name", Optional.empty())
                .mapToBean(Something.class)
                .list();

        assertThat(result.size(), equalTo(2));

        assertEquals(1, result.get(0).getId());
        assertEquals("eric", result.get(0).getName());

        assertEquals(2, result.get(1).getId());
        assertEquals("brian", result.get(1).getName());
    }

    @Test
    public void testBindOptionalPresent() throws Exception {
        Something result = handle.createQuery(SELECT_BY_NAME)
                .bind("name", Optional.of("brian"))
                .mapToBean(Something.class)
                .findOnly();

        assertEquals(2, result.getId());
        assertEquals("brian", result.getName());
    }

    @Test
    public void testBindOptionalEmpty() throws Exception {
        List<Something> result = handle.createQuery(SELECT_BY_NAME)
                .bind("name", Optional.empty())
                .mapToBean(Something.class)
                .list();

        assertThat(result.size(), equalTo(2));

        assertEquals(1, result.get(0).getId());
        assertEquals("eric", result.get(0).getName());

        assertEquals(2, result.get(1).getId());
        assertEquals("brian", result.get(1).getName());
    }

}
