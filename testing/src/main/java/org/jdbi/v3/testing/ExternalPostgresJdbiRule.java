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
package org.jdbi.v3.testing;

import javax.sql.DataSource;

import org.postgresql.ds.PGSimpleDataSource;

class ExternalPostgresJdbiRule extends JdbiRule {

    private final String hostname;
    private final Integer port;
    private final String username;
    private final String password;
    private final String database;

    ExternalPostgresJdbiRule(
            String hostname,
            Integer port,
            String username,
            String password,
            String database) {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
        this.database = database;
    }

    @Override
    protected DataSource createDataSource() {
        final PGSimpleDataSource datasource = new PGSimpleDataSource();
        datasource.setServerName(hostname);
        datasource.setPortNumber(port);
        datasource.setUser(username);
        datasource.setPassword(password);
        datasource.setDatabaseName(database);
        datasource.setApplicationName("Hermes Unit Tests");
        return datasource;
    }

}
