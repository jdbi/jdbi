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

package com.nebhale.r2dbc.postgresql;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class PostgresqlConnectionConfigurationTest {

    @Test
    public void builderNoApplicationName() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlConnectionConfiguration.builder().applicationName(null))
            .withMessage("applicationName must not be null");
    }

    @Test
    public void builderNoHost() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlConnectionConfiguration.builder().host(null))
            .withMessage("host must not be null");
    }

    @Test
    public void builderNoPassword() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlConnectionConfiguration.builder().password(null))
            .withMessage("password must not be null");
    }

    @Test
    public void builderNoUsername() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlConnectionConfiguration.builder().username(null))
            .withMessage("username must not be null");
    }

    @Test
    public void configuration() {
        PostgresqlConnectionConfiguration configuration = PostgresqlConnectionConfiguration.builder()
            .applicationName("test-application-name")
            .database("test-database")
            .host("test-host")
            .password("test-password")
            .port(100)
            .username("test-username")
            .build();

        assertThat(configuration)
            .hasFieldOrPropertyWithValue("applicationName", "test-application-name")
            .hasFieldOrPropertyWithValue("database", "test-database")
            .hasFieldOrPropertyWithValue("host", "test-host")
            .hasFieldOrPropertyWithValue("password", "test-password")
            .hasFieldOrPropertyWithValue("port", 100)
            .hasFieldOrPropertyWithValue("username", "test-username");
    }

    @Test
    public void configurationDefaults() {
        PostgresqlConnectionConfiguration configuration = PostgresqlConnectionConfiguration.builder()
            .database("test-database")
            .host("test-host")
            .password("test-password")
            .username("test-username")
            .build();

        assertThat(configuration)
            .hasFieldOrPropertyWithValue("applicationName", "postgresql-r2dbc")
            .hasFieldOrPropertyWithValue("database", "test-database")
            .hasFieldOrPropertyWithValue("host", "test-host")
            .hasFieldOrPropertyWithValue("password", "test-password")
            .hasFieldOrPropertyWithValue("port", 5432)
            .hasFieldOrPropertyWithValue("username", "test-username");
    }

    @Test
    public void constructorNoNoHost() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlConnectionConfiguration.builder()
            .password("test-password")
            .username("test-username")
            .build())
            .withMessage("host must not be null");
    }

    @Test
    public void constructorNoPassword() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlConnectionConfiguration.builder()
            .host("test-host")
            .username("test-username")
            .build())
            .withMessage("password must not be null");
    }

    @Test
    public void constructorNoUsername() {
        assertThatNullPointerException().isThrownBy(() -> PostgresqlConnectionConfiguration.builder()
            .host("test-host")
            .password("test-password")
            .build())
            .withMessage("username must not be null");
    }
}
