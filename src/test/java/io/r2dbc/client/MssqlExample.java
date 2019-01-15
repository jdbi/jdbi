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
import io.r2dbc.spi.ConnectionFactories;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MSSQLServerContainer;
import reactor.util.annotation.Nullable;

import java.io.IOException;

import static io.r2dbc.mssql.MssqlConnectionFactoryProvider.MSSQL_DRIVER;
import static io.r2dbc.spi.ConnectionFactoryOptions.DRIVER;
import static io.r2dbc.spi.ConnectionFactoryOptions.HOST;
import static io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD;
import static io.r2dbc.spi.ConnectionFactoryOptions.PORT;
import static io.r2dbc.spi.ConnectionFactoryOptions.USER;
import static io.r2dbc.spi.ConnectionFactoryOptions.builder;

final class MssqlExample implements Example<String> {

    @RegisterExtension
    static final MssqlServerExtension SERVER = new MssqlServerExtension();

    private final R2dbc r2dbc = new R2dbc(ConnectionFactories.get(builder()
        .option(DRIVER, MSSQL_DRIVER)
        .option(HOST, SERVER.getHost())
        .option(PORT, SERVER.getPort())
        .option(PASSWORD, SERVER.getPassword())
        .option(USER, SERVER.getUsername())
        .build()));


    @Override
    public String getIdentifier(int index) {
        return String.format("P%d", index);
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
        return String.format("@P%d", index);
    }

    @Override
    public R2dbc getR2dbc() {
        return this.r2dbc;
    }

    private static final class MssqlServerExtension implements BeforeAllCallback, AfterAllCallback {

        private final MSSQLServerContainer<?> container = new MSSQLServerContainer<>();

        private HikariDataSource dataSource;

        private JdbcOperations jdbcOperations;

        @Override
        public void afterAll(ExtensionContext context) {
            this.dataSource.close();
            this.container.stop();
        }

        @Override
        public void beforeAll(ExtensionContext context) throws IOException {
            this.container.start();

            this.dataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(this.container.getJdbcUrl())
                .username(this.container.getUsername())
                .password(this.container.getPassword())
                .build();

            this.dataSource.setMaximumPoolSize(1);

            this.jdbcOperations = new JdbcTemplate(this.dataSource);
        }

        String getHost() {
            return this.container.getContainerIpAddress();
        }

        @Nullable
        JdbcOperations getJdbcOperations() {
            return this.jdbcOperations;
        }

        String getPassword() {
            return this.container.getPassword();
        }

        int getPort() {
            return this.container.getMappedPort(MSSQLServerContainer.MS_SQL_SERVER_PORT);
        }

        String getUsername() {
            return this.container.getUsername();
        }

    }
}
