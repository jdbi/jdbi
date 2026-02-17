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

    private final String url;

    private String user = null;
    private String password = null;

    static JdbiExtension instance() {
        return new JdbiH2Extension();
    }

    static JdbiExtension instance(String options) {
        return new JdbiH2Extension(options);
    }

    public JdbiH2Extension() {
        this("");
    }

    /**
     * Allows setting the options string for the H2 database. The value here is
     * appended to the database URL.
     *
     * @param options The options string. Must not be null. May start with a semicolon.
     */
    public JdbiH2Extension(String options) {
        StringBuilder url = new StringBuilder("jdbc:h2:mem:")
                .append(UUID.randomUUID());

        if (!options.isEmpty()) {
            if (!options.startsWith(";")) {
                url.append(';');
            }
            url.append(options);
        }

        this.url = url.toString();
    }

    /**
     * Sets the H2 username.
     *
     * @param user The username. Can be null.
     * @return This object.
     */
    public JdbiH2Extension withUser(String user) {
        this.user = user;

        return this;
    }

    /**
     * Sets the H2 username and password.
     *
     * @param user     The username. Can be null.
     * @param password The password. Can be null.
     * @return This object.
     */
    public JdbiH2Extension withCredentials(String user, String password) {
        this.user = user;
        this.password = password;

        return this;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    protected DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL(getUrl());

        if (user != null) {
            ds.setUser(user);

            if (password != null) {
                ds.setPassword(password);
            }
        }

        return ds;
    }
}
