/*
 * Copyright 2017-2018 the original author or authors.
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
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import reactor.util.annotation.Nullable;

import java.io.IOException;

final class PostgresqlServerExtension implements BeforeAllCallback, AfterAllCallback {

    private final PostgreSQLContainer<?> container = new PostgreSQLContainer<>();

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

        this.jdbcOperations = new JdbcTemplate(this.dataSource);
    }

    String getDatabase() {
        return this.container.getDatabaseName();
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
        return this.container.getMappedPort(5432);
    }

    String getUsername() {
        return this.container.getUsername();
    }

}
