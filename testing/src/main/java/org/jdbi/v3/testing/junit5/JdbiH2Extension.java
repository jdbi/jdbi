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

import java.util.UUID;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;

/**
 * Jdbi H2 JUnit 5 rule.
 *
 * Override methods for special case construction:
 *
 * <pre>{@code
 *     @RegisterExtension
 *     public JdbiExtension extension = new JdbiH2Extension() {
 *         @Override
 *         protected DataSource createDataSource() {
 *            ...
 *         }
 *     };
 * }</pre>
 *
 * Use with {@link org.junit.jupiter.api.extension.ExtendWith}:
 *
 * <pre>{@code
 * @ExtendWith(JdbiH2Extension.class)
 * public class DatabaseTest {
 *     @Test
 *     public void testWithJdbi(Jdbi jdbi) {
 *         ...
 *     }
 *
 *     @Test
 *     public void testWithHandle(Handle handle) {
 *         ...
 *     }
 * }
 * }</pre>
 */
public class JdbiH2Extension extends JdbiExtension {

    private final String url = "jdbc:h2:mem:" + UUID.randomUUID();

    static JdbiExtension instance() {
        return new JdbiH2Extension();
    }

    public JdbiH2Extension() {}

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    protected DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL(getUrl());

        return ds;
    }
}
