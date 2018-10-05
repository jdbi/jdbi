/*
 * Copyright 2018 the original author or authors.
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

import javax.sql.DataSource;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.HikariDataSource;

/**
 * @author Greg Turnquist
 */
final class H2ServerExtension implements BeforeAllCallback, AfterAllCallback {

	private static final Logger log = LoggerFactory.getLogger(H2ServerExtension.class);

	private final String connectionUrl;

	private JdbcOperations jdbcOperations;

	public H2ServerExtension(String connectionUrl) {
		this.connectionUrl = connectionUrl;
	}

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {

		log.info("H2 database starting...");

		DataSource anotherDataSource = DataSourceBuilder.create()
			.type(HikariDataSource.class)
			.driverClassName("org.h2.Driver").url(this.connectionUrl)
			.username("sa").password("")
			.build();

		this.jdbcOperations = new JdbcTemplate(anotherDataSource);
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		log.info("H2 database stopping...");
	}

	JdbcOperations getJdbcOperations() {
		return jdbcOperations;
	}
}
