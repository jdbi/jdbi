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
package org.jdbi.testing.junit;

import org.jdbi.core.statement.Query;
import org.jdbi.core.statement.Update;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import static java.lang.String.format;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(OrderAnnotation.class)
public class TestJdbiExtensionScopes {

    @RegisterExtension
    public static JdbiExtension perClassInstance = JdbiExtension.h2();

    @RegisterExtension
    public JdbiExtension perTestInstance = JdbiExtension.h2();

    @Test
    @Order(1)
    public void testTableCreation() {
        // create tables in a static (perClass) and a member (perTest) instance
        assertThat(createTable(perClassInstance, "TABLE1")).isZero();
        assertThat(existsTable(perClassInstance, "TABLE1")).isTrue();

        // ensure that the tables exist
        assertThat(createTable(perTestInstance, "TABLE1")).isZero();
        assertThat(existsTable(perTestInstance, "TABLE1")).isTrue();
    }

    @Test
    @Order(2)
    public void testTableExists() {
        // perClassInstance is still the same as in testTableCreation, so the table is still here
        assertThat(existsTable(perClassInstance, "TABLE1")).isTrue();
        // perTestInstance got reset between tests, so the table is gone.
        assertThat(existsTable(perTestInstance, "TABLE1")).isFalse();
    }


    static int createTable(JdbiExtension extension, String table) {
        try (Update update = extension.getSharedHandle().createUpdate(format("CREATE TABLE public.%s (a INTEGER)", table))) {
            return update.execute();
        }
    }

    static boolean existsTable(JdbiExtension extension, String table) {
        try (Query query = extension.getSharedHandle()
            .createQuery(format("SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'PUBLIC' AND table_name = '%s')", table))) {
            return query.mapTo(Boolean.class).one();
        }
    }
}
