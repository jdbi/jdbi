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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestConcurrentUpdatingQuery {
    @Rule
    public JdbiRule dbRule = JdbiRule.h2();

    private Handle handle;

    @Before
    public void setUp() throws Exception {
        handle = dbRule.getHandle();
    }

    @Test
    public void testConcurrentUpdateableResultSet() throws Exception {
        handle.execute("create table something (id identity primary key, name varchar(50))");
        handle.execute("insert into something (id, name) values (7, 'Tim')");
        handle.createQuery("select id, name from something where id = :id")
                .bind("id", 7)
                .concurrentUpdatable()
                .map((r, ctx) -> {
                    r.updateString("name", "Tom");
                    r.updateRow();
                    return null;
                }).list();

        final String name = handle.createQuery("select name from something where id = :id")
                .bind("id", 7)
                .mapTo(String.class)
                .findOnly();

        assertThat(name).isEqualTo("Tom");
    }
}
