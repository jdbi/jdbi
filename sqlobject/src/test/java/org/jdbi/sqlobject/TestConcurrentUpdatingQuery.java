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
package org.jdbi.sqlobject;

import org.jdbi.core.Handle;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestConcurrentUpdatingQuery {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.openHandle();
    }

    @AfterEach
    public void tearDown() {
        handle.close();
    }

    @Test
    public void testConcurrentUpdateableResultSet() {
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
                .one();

        assertThat(name).isEqualTo("Tom");
    }
}
