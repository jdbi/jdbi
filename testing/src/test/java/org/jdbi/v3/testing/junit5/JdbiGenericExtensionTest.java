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
package org.jdbi.v3.testing.junit5;

import java.util.List;
import java.util.UUID;

import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledForJreRange(min = JRE.JAVA_17)
public class JdbiGenericExtensionTest {

    @RegisterExtension
    public JdbiExtension derbyExtension = new JdbiGenericExtension("jdbc:derby:memory:" + UUID.randomUUID().toString() + ";create=true")
            .withInitializer(TestingInitializers.usersWithData());

    @RegisterExtension
    public JdbiExtension hsqldbExtension = new JdbiGenericExtension("jdbc:hsqldb:mem:" + UUID.randomUUID())
            .withCredentials("username", "password")
            .withInitializer(TestingInitializers.usersWithData());

    @Test
    public void testApacheDerby() {
        List<String> userNames = derbyExtension.getJdbi().withHandle(h -> {
            try (Query query = h.createQuery("SELECT name FROM users ORDER BY id")) {
                return query.mapTo(String.class).list();
            }
        });

        assertThat(userNames).hasSize(2);
        assertThat(userNames).containsExactly("Alice", "Bob");
    }

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
