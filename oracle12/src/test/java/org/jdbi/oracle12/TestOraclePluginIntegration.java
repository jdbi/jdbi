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
package org.jdbi.oracle12;

import org.jdbi.core.Handle;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit5.tc.JdbiTestcontainersExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link OraclePlugin} against a real Oracle database.
 * Verifies that binding untyped null values works correctly with the plugin.
 */
@Tag("slow")
@Testcontainers
public class TestOraclePluginIntegration {

    static final String CONTAINER_VERSION = "gvenzl/oracle-free:" + System.getProperty("oracle.container.version", "slim-faststart");

    @Container
    static OracleContainer oc = new OracleContainer(CONTAINER_VERSION);

    @RegisterExtension
    public JdbiExtension oracleExtension = JdbiTestcontainersExtension.instance(oc)
            .withPlugin(new OraclePlugin());

    @BeforeEach
    public void beforeEach() {
        Handle handle = oracleExtension.getSharedHandle();
        handle.execute("create table things (id integer, name varchar(200))");
    }

    @Test
    public void testInsertWithUntypedNull() {
        Handle h = oracleExtension.getSharedHandle();

        // insert a row with an untyped null value - this fails on Oracle
        // without the OraclePlugin because the default Types.OTHER is not supported
        h.execute("insert into things (id, name) values (?, ?)", 1, null);

        String name = h.createQuery("select name from things where id = 1")
                .mapTo(String.class)
                .one();
        assertThat(name).isNull();
    }

    @Test
    public void testUpdateWithUntypedNull() {
        Handle h = oracleExtension.getSharedHandle();

        h.execute("insert into things (id, name) values (?, ?)", 1, "Alice");
        h.execute("update things set name = ? where id = ?", null, 1);

        String name = h.createQuery("select name from things where id = 1")
                .mapTo(String.class)
                .one();
        assertThat(name).isNull();
    }

    @Test
    public void testQueryWithUntypedNull() {
        Handle h = oracleExtension.getSharedHandle();

        h.execute("insert into things (id, name) values (?, ?)", 1, "Alice");
        h.execute("insert into things (id, name) values (?, ?)", 2, null);

        // query using an untyped null in a where clause
        assertThat(h.createQuery("select id from things where name is null")
                .mapTo(Integer.class)
                .one())
                .isEqualTo(2);
    }
}
