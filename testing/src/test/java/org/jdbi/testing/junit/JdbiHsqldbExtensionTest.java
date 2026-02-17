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

import java.util.List;
import java.util.UUID;

import org.jdbi.core.statement.Query;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class JdbiHsqldbExtensionTest {

    @RegisterExtension
    public JdbiExtension hsqldbExtension = new JdbiGenericExtension("jdbc:hsqldb:mem:" + UUID.randomUUID())
            .withCredentials("username", "password")
            .withInitializer(TestingInitializers.usersWithData());

    @Test
    public void testHsqldb() {
        List<String> userNames = hsqldbExtension.getJdbi().withHandle(h -> {
            try (Query query = h.createQuery("SELECT name FROM users ORDER BY id")) {
                return query.mapTo(String.class).list();
            }
        });

        assertThat(userNames).hasSize(2);
        assertThat(userNames).containsExactly("Alice", "Bob");
    }
}
