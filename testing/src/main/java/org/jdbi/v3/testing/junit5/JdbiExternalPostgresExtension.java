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

import javax.sql.DataSource;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.postgresql.ds.PGSimpleDataSource;

import static java.util.Objects.requireNonNull;

public class JdbiExternalPostgresExtension extends JdbiExtension {

    private final String hostname;
    private final Integer port;
    private final String username;
    private final String password;
    private final String database;

    private final String jdbcUri;

    /**
     * Jdbi external PostgreSQL JUnit 5 rule. This class should not be used directly but through {@link JdbiExtension#externalPostgres(String, Integer, String, String, String)}.
     * <br>
     * This class is public with a protected constructor to allow special case construction.
     *
     * <pre>{@code
     *     @RegisterExtension
     *     public JdbiExtension extension = new JdbiExternalPostgresExtension(...) {
     *         @Override
     *         protected DataSource createDataSource() {
     *            ...
     *         }
     *     };
     * }</pre>
     */
    protected JdbiExternalPostgresExtension(@Nonnull String hostname,
        @Nullable Integer port,
        @Nonnull String database,
        @Nullable String username,
        @Nullable String password) {

        this.hostname = requireNonNull(hostname, "hostname is null");
        this.port = port;

        this.database = requireNonNull(database, "database is null");

        this.username = username;
        this.password = password;

        StringBuilder sb = new StringBuilder("jdbc:postgresql://");
        sb.append(this.hostname);
        if (port != null) {
            sb.append(':').append(port);
        }
        sb.append("database");
        if (username != null) {
            sb.append("?user=").append(username);
        }

        this.jdbcUri = sb.toString();
    }

    static JdbiExtension instance(@Nonnull String hostname,
        @Nullable Integer port,
        @Nonnull String database,
        @Nullable String username,
        @Nullable String password) {
        return new JdbiExternalPostgresExtension(hostname, port, database, username, password);
    }

    @Override
    public String getUrl() {
        return jdbcUri;
    }

    @Override
    protected DataSource createDataSource() throws Exception {
        final PGSimpleDataSource datasource = new PGSimpleDataSource();

        datasource.setServerNames(new String[]{hostname});

        if (port != null) {
            datasource.setPortNumbers(new int[]{port});
        }

        datasource.setUser(username);
        datasource.setPassword(password);

        datasource.setDatabaseName(database);
        datasource.setApplicationName(this.getClass().getSimpleName());

        return datasource;

    }
}
