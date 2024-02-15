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
package org.jdbi.v3.testing.junit5.tc;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jdbi.v3.meta.Alpha;
import org.jdbi.v3.meta.Beta;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.testcontainers.containers.JdbcDatabaseContainer;

import static java.lang.String.format;

/**
 * Support <a href="https://testcontainers.org/">Testcontainer JDBC containers</a> as database for Jdbi tests.
 */
@Beta
public final class JdbiTestcontainersExtension extends JdbiExtension {

    private final JdbcDatabaseContainer<?> jdbcDatabaseContainer;
    private final TestcontainersDatabaseInformation databaseInformation;
    private final TestcontainersDatabaseInformationSupplier instanceProvider;

    private volatile HikariDataSource masterDatasource = null;
    private volatile HikariConfig masterConfig = null;
    private volatile HikariDataSource schemaDatasource = null;

    /**
     * Create a new {@link JdbiExtension} that uses the supplied {@link JdbcDatabaseContainer} as database. This must be a supported
     * container instance.
     *
     * @param jdbcDatabaseContainer A supported {@link JdbcDatabaseContainer} instance.
     * @return An initialized {@link JdbiExtension} instance that uses the database container.
     * @throws IllegalArgumentException If the provided container class is not supported.
     */
    public static JdbiTestcontainersExtension instance(JdbcDatabaseContainer<?> jdbcDatabaseContainer) {
        TestcontainersDatabaseInformation databaseInformation = TestcontainersDatabaseInformation.forTestcontainerClass(jdbcDatabaseContainer.getClass());

        if (databaseInformation == null) {
            throw new IllegalArgumentException(format("Container class '%s' is unknown!", jdbcDatabaseContainer.getClass().getName()));
        }

        return new JdbiTestcontainersExtension(databaseInformation, jdbcDatabaseContainer);
    }

    /**
     * Create a new {@link JdbiExtension} that uses the supplied {@link JdbcDatabaseContainer} as database.
     *
     * @param databaseInformation A {@link TestcontainersDatabaseInformation} instance that describes how to create new test-isolation databases or schemata.
     * @param jdbcDatabaseContainer A {@link JdbcDatabaseContainer} instance.
     * @return An initialized {@link JdbiExtension} instance that uses the database container.
     */
    public static JdbiTestcontainersExtension instance(TestcontainersDatabaseInformation databaseInformation, JdbcDatabaseContainer<?> jdbcDatabaseContainer) {
        return new JdbiTestcontainersExtension(databaseInformation, jdbcDatabaseContainer);
    }

    private JdbiTestcontainersExtension(TestcontainersDatabaseInformation databaseInformation, JdbcDatabaseContainer<?> jdbcDatabaseContainer) {
        this.jdbcDatabaseContainer = jdbcDatabaseContainer;
        this.databaseInformation = databaseInformation;
        this.instanceProvider = new TestcontainersDatabaseInformationSupplier(databaseInformation);
    }

    /**
     * Sets the maximum wait time for shutting down the extension.
     * @see TestcontainersDatabaseInformation#setShutdownWaitTimeInSeconds(int)
     *
     * @since 3.45.0
     */
    @Alpha
    public JdbiTestcontainersExtension setShutdownWaitTimeInSeconds(int seconds) {
        this.databaseInformation.setShutdownWaitTimeInSeconds(seconds);

        return this;
    }

    @Override
    public String getUrl() {
        return jdbcDatabaseContainer.getJdbcUrl();
    }

    @Override
    protected DataSource createDataSource() {

        if (masterDatasource == null || masterConfig == null) {
            throw new IllegalArgumentException("Extension was not started!");
        }

        TestcontainersDatabaseInformation databaseInformation = instanceProvider.get();

        HikariConfig schemaConfig = new HikariConfig();
        masterConfig.copyStateTo(schemaConfig);

        databaseInformation.getCatalog().ifPresent(schemaConfig::setCatalog);
        databaseInformation.getSchema().ifPresent(schemaConfig::setSchema);
        schemaConfig.setPoolName(format("jdbi-test-pool (%s)", databaseInformation));

        try (HikariDataSource ds = schemaDatasource) {
            this.schemaDatasource = new HikariDataSource(schemaConfig);
        }

        return schemaDatasource;
    }

    @Override
    protected void startExtension() throws Exception {

        if (masterDatasource != null || masterConfig != null) {
            throw new IllegalArgumentException("Extension was already started!");
        }

        masterConfig = new HikariConfig();
        masterConfig.setJdbcUrl(jdbcDatabaseContainer.getJdbcUrl());
        masterConfig.setUsername(databaseInformation.getUser().orElse(jdbcDatabaseContainer.getUsername()));
        masterConfig.setPassword(jdbcDatabaseContainer.getPassword());
        masterConfig.setDriverClassName(jdbcDatabaseContainer.getDriverClassName());
        masterConfig.setPoolName(format("jdbi-template-pool (%s)", databaseInformation));

        databaseInformation.getCatalog().ifPresent(masterConfig::setCatalog);
        databaseInformation.getSchema().ifPresent(masterConfig::setSchema);

        this.masterDatasource = new HikariDataSource(masterConfig);

        this.instanceProvider.start(masterDatasource);

        super.startExtension();
    }

    @Override
    protected void stopExtension() throws Exception {

        if (masterDatasource == null || masterConfig == null) {
            throw new IllegalArgumentException("Extension was not started!");
        }

        try (HikariDataSource masterDs = masterDatasource;
            HikariDataSource schemaDs = schemaDatasource;
            TestcontainersDatabaseInformationSupplier ip = instanceProvider) {
            super.stopExtension();
        }
    }
}
