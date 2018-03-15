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


import com.nebhale.r2dbc.core.nullability.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Connection configuration information for connecting to a PostgreSQL database.
 */
public final class PostgresqlConnectionConfiguration {

    private final String applicationName;

    private final String database;

    private final String host;

    private final String password;

    private final int port;

    private final String username;

    private PostgresqlConnectionConfiguration(String applicationName, @Nullable String database, String host, String password, int port, String username) {
        this.applicationName = Objects.requireNonNull(applicationName);
        this.database = database;
        this.host = Objects.requireNonNull(host, "host must not be null");
        this.password = Objects.requireNonNull(password, "password must not be null");
        this.port = port;
        this.username = Objects.requireNonNull(username, "username must not be null");
    }

    /**
     * Returns a new {@link Builder}.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "PostgresConnectionConfiguration{" +
            "applicationName='" + this.applicationName + '\'' +
            ", database='" + this.database + '\'' +
            ", host='" + this.host + '\'' +
            ", password='" + this.password + '\'' +
            ", port=" + this.port +
            ", username='" + this.username + '\'' +
            '}';
    }

    String getApplicationName() {
        return this.applicationName;
    }

    Optional<String> getDatabase() {
        return Optional.ofNullable(this.database);
    }

    String getHost() {
        return this.host;
    }

    String getPassword() {
        return this.password;
    }

    int getPort() {
        return this.port;
    }

    String getUsername() {
        return this.username;
    }

    /**
     * A builder for {@link PostgresqlConnectionConfiguration} instances.
     * <p>
     * <i>This class is not threadsafe</i>
     */
    public static final class Builder {

        private String applicationName = "postgresql-r2dbc";

        private String database;

        private String host;

        private String password;

        private int port = 5432;

        private String username;

        private Builder() {
        }

        /**
         * Configure the application name.  Defaults to {@code postgresql-r2dbc}.
         *
         * @param applicationName the application name
         * @return this {@link Builder}
         * @throws NullPointerException if {@code applicationName} is {@code null}
         */
        public Builder applicationName(String applicationName) {
            this.applicationName = Objects.requireNonNull(applicationName, "applicationName must not be null");
            return this;
        }

        /**
         * Returns a configured {@link PostgresqlConnectionConfiguration}.
         *
         * @return a configured {@link PostgresqlConnectionConfiguration}
         */
        public PostgresqlConnectionConfiguration build() {
            return new PostgresqlConnectionConfiguration(this.applicationName, this.database, this.host, this.password, this.port, this.username);
        }

        /**
         * Configure the database.
         *
         * @param database the database
         * @return this {@link Builder}
         */
        public Builder database(@Nullable String database) {
            this.database = database;
            return this;
        }

        /**
         * Configure the host.
         *
         * @param host the host
         * @return this {@link Builder}
         * @throws NullPointerException if {@code host} is {@code null}
         */
        public Builder host(String host) {
            this.host = Objects.requireNonNull(host, "host must not be null");
            return this;
        }

        /**
         * Configure the password.
         *
         * @param password the password
         * @return this {@link Builder}
         * @throws NullPointerException if {@code password} is {@code null}
         */
        public Builder password(String password) {
            this.password = Objects.requireNonNull(password, "password must not be null");
            return this;
        }

        /**
         * Configure the port.  Defaults to {@code 5432}.
         *
         * @param port the port
         * @return this {@link Builder}
         */
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        @Override
        public String toString() {
            return "Builder{" +
                "applicationName='" + this.applicationName + '\'' +
                ", database='" + this.database + '\'' +
                ", host='" + this.host + '\'' +
                ", password='" + this.password + '\'' +
                ", port=" + this.port +
                ", username='" + this.username + '\'' +
                '}';
        }

        /**
         * Configure the username.
         *
         * @param username the username
         * @return this {@link Builder}
         * @throws NullPointerException if {@code username} is {@code null}
         */
        public Builder username(String username) {
            this.username = Objects.requireNonNull(username, "username must not be null");
            return this;
        }

    }

}
