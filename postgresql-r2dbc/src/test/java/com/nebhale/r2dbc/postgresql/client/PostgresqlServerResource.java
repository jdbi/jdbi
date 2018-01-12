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

package com.nebhale.r2dbc.postgresql.client;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.SocketUtils;
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static ru.yandex.qatools.embed.postgresql.EmbeddedPostgres.cachedRuntimeConfig;

public final class PostgresqlServerResource extends ExternalResource {

    private final Logger logger = LoggerFactory.getLogger("test.postgresql-server");

    private final String database;

    private final String host;

    private final String password;

    private final int port;

    private final EmbeddedPostgres server;

    private final String username;

    private HikariDataSource dataSource;

    private JdbcOperations jdbcOperations;

    public PostgresqlServerResource() {
        this.database = randomAlphanumeric(8);
        this.host = "localhost";
        this.password = randomAlphanumeric(16);
        this.port = SocketUtils.findAvailableTcpPort();
        this.username = randomAlphanumeric(16);

        this.server = new EmbeddedPostgres(getDataDirectory(this.database));
    }

    public String getDatabase() {
        return this.database;
    }

    public String getHost() {
        return this.host;
    }

    public JdbcOperations getJdbcOperations() {
        return this.jdbcOperations;
    }

    public String getPassword() {
        return this.password;
    }

    public int getPort() {
        return this.port;
    }

    public String getUsername() {
        return this.username;
    }

    @Override
    protected void after() {
        this.logger.info("PostgreSQL server stopping");
        this.dataSource.close();
        this.server.stop();
        this.logger.info("PostgreSQL server stopped");
    }

    @Override
    protected void before() throws Throwable {
        this.logger.info("PostgreSQL server starting");
        this.server.start(cachedRuntimeConfig(getCachePath()), this.host, this.port, this.database, this.username, this.password, Collections.emptyList());

        this.dataSource = this.server.getConnectionUrl()
            .map(url -> DataSourceBuilder.create().type(HikariDataSource.class).url(url).build())
            .orElseThrow(() -> new IllegalStateException("Unable to determine JDBC URI"));

        this.jdbcOperations = new JdbcTemplate(this.dataSource);

        this.logger.info("PostgreSQL server started");
    }

    private static Path getCachePath() {
        return Paths.get("target/postgresql");
    }

    private static String getDataDirectory(String database) {
        return String.format("target/postgresql/data/%s", database);
    }

}
