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
package org.jdbi.testing.junit5;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * Very simple implementation of a JUnit 5 {@link JdbiExtension} which can handle any JDBC connection.
 */
@SuppressWarnings("HiddenField")
public class JdbiGenericExtension extends JdbiExtension {

    private final String jdbcUri;

    private String user = null;
    private String password = null;

    /**
     * Creates a new instance for a given JDBC URI. The driver for the database must be on the classpath!
     *
     * @param jdbcUri A JDBC URI.
     */
    public JdbiGenericExtension(String jdbcUri) {
        this.jdbcUri = jdbcUri;
    }

    @Override
    public String getUrl() {
        return jdbcUri;
    }

    /**
     * Sets the username.
     *
     * @param user The username. Can be null.
     * @return This object.
     */
    public JdbiGenericExtension withUser(String user) {
        this.user = user;

        return this;
    }

    /**
     * Sets the username and password.
     *
     * @param user     The username. Can be null.
     * @param password The password. Can be null.
     * @return This object.
     */
    public JdbiGenericExtension withCredentials(String user, String password) {
        this.user = user;
        this.password = password;

        return this;
    }

    @Override
    protected DataSource createDataSource() throws Exception {

        Driver driver = DriverManager.getDriver(jdbcUri);
        Properties info = new Properties();
        if (user != null) {
            info.put("user", user);
            if (password != null) {
                info.put("password", password);
            }
        }

        return new JdbiDataSource(driver, info);
    }

    final class JdbiDataSource implements DataSource {

        private final Driver driver;
        private final Properties rootProperties;

        JdbiDataSource(Driver driver, Properties rootProperties) {
            this.driver = driver;
            this.rootProperties = rootProperties;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return driver.connect(jdbcUri, rootProperties);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            Properties connectionProperties = new Properties(rootProperties);
            if (username != null) {
                connectionProperties.put("user", username);
                if (password != null) {
                    connectionProperties.put("password", password);
                }
            }
            return driver.connect(jdbcUri, connectionProperties);
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            throw new UnsupportedOperationException("getLogWriter");
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {
            throw new UnsupportedOperationException("setLogWriter");
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            throw new UnsupportedOperationException("setLoginTimeout");
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new UnsupportedOperationException("getParentLogger");
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException();
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return false;
        }
    }
}
