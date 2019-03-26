/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import reactor.util.annotation.Nullable;

import static com.github.jasync.r2dbc.mysql.MysqlConnectionFactoryProvider.MYSQL_DRIVER;
import static io.r2dbc.spi.ConnectionFactoryOptions.DATABASE;
import static io.r2dbc.spi.ConnectionFactoryOptions.DRIVER;
import static io.r2dbc.spi.ConnectionFactoryOptions.HOST;
import static io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD;
import static io.r2dbc.spi.ConnectionFactoryOptions.PORT;
import static io.r2dbc.spi.ConnectionFactoryOptions.USER;
import static io.r2dbc.spi.ConnectionFactoryOptions.builder;

final class MysqlExample implements Example<String> {

    @RegisterExtension
    static final MysqlServerExtension SERVER = new MysqlServerExtension();

    private final R2dbc r2dbc = new R2dbc(ConnectionFactories.get(builder()
            .option(DRIVER, MYSQL_DRIVER)
            .option(HOST, SERVER.getHost())
            .option(PORT, SERVER.getPort())
            .option(PASSWORD, SERVER.getPassword())
            .option(USER, SERVER.getUsername())
            .option(DATABASE, SERVER.getDatabase())
            .build()));


    @Override
    public String getIdentifier(int index) {
        return String.valueOf(index);
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

    @Override
    public R2dbc getR2dbc() {
        return this.r2dbc;
    }

    private static final class MysqlServerExtension implements BeforeAllCallback, AfterAllCallback {

        private final MySQLContainer<?> container = new MySQLContainer<>("mysql:5.7");

        private HikariDataSource dataSource;

        private JdbcOperations jdbcOperations;

        @Override
        public void afterAll(ExtensionContext context) {
            this.dataSource.close();
            this.container.stop();
        }

        @Override
        public void beforeAll(ExtensionContext context) {
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
            return this.container.getFirstMappedPort();
        }

        String getUsername() {
            return this.container.getUsername();
        }

        String getDatabase() {
            return this.container.getDatabaseName();
        }
    }

    @Test
    @Ignore("compound statements are not supported by the driver")
    @Override
    public void compoundStatement() {
    }
}
