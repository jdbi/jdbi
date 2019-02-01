/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.r2dbc.client;

import com.zaxxer.hikari.HikariDataSource;
import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.test.Example;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import reactor.util.annotation.Nullable;

import static io.r2dbc.h2.H2ConnectionFactoryProvider.H2_DRIVER;
import static io.r2dbc.h2.H2ConnectionFactoryProvider.URL;
import static io.r2dbc.spi.ConnectionFactoryOptions.DRIVER;
import static io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD;
import static io.r2dbc.spi.ConnectionFactoryOptions.USER;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

final class H2Example {

    @RegisterExtension
    static final H2ServerExtension SERVER = new H2ServerExtension();

    private final ConnectionFactory connectionFactory = ConnectionFactories.get(ConnectionFactoryOptions.builder()
        .option(DRIVER, H2_DRIVER)
        .option(PASSWORD, SERVER.getPassword())
        .option(URL, SERVER.getUrl())
        .option(USER, SERVER.getUsername())
        .build());

    private static final class H2ServerExtension implements BeforeAllCallback, AfterAllCallback {

        private final String password = randomAlphanumeric(16);

        private final String url = String.format("mem:%s", randomAlphanumeric(8));

        private final String username = randomAlphanumeric(16);

        private HikariDataSource dataSource;

        private JdbcOperations jdbcOperations;

        @Override
        public void afterAll(ExtensionContext context) {
            this.dataSource.close();
        }

        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            this.dataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(String.format("jdbc:h2:%s;USER=%s;PASSWORD=%s;DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=4", this.url, this.username, this.password))
                .build();

            this.dataSource.setMaximumPoolSize(1);

            this.jdbcOperations = new JdbcTemplate(this.dataSource);
        }

        @Nullable
        JdbcOperations getJdbcOperations() {
            return this.jdbcOperations;
        }

        String getPassword() {
            return this.password;
        }

        String getUrl() {
            return this.url;
        }

        String getUsername() {
            return this.username;
        }

    }

    // TODO: Remove once implemented
    @Disabled("Not yet implemented")
    @Nested
    final class JdbcStyle implements io.r2dbc.spi.test.Example<Integer> {

        @Override
        public ConnectionFactory getConnectionFactory() {
            return H2Example.this.connectionFactory;
        }

        @Override
        public Integer getIdentifier(int index) {
            return index;
        }

        @Override
        public JdbcOperations getJdbcOperations() {
            JdbcOperations jdbcOperations = SERVER.getJdbcOperations();

            if (jdbcOperations == null) {
                throw new IllegalStateException("JdbcOperations not yet initialized");
            }

            return jdbcOperations;
        }

        @Override
        public String getPlaceholder(int index) {
            return "?";
        }
    }

    @Nested
    final class PostgresqlStyle implements Example<String> {

        @Override
        public ConnectionFactory getConnectionFactory() {
            return H2Example.this.connectionFactory;
        }

        @Override
        public String getIdentifier(int index) {
            return getPlaceholder(index);
        }

        @Override
        public JdbcOperations getJdbcOperations() {
            JdbcOperations jdbcOperations = SERVER.getJdbcOperations();

            if (jdbcOperations == null) {
                throw new IllegalStateException("JdbcOperations not yet initialized");
            }

            return jdbcOperations;
        }

        @Override
        public String getPlaceholder(int index) {
            return String.format("$%d", index + 1);
        }

    }

}
