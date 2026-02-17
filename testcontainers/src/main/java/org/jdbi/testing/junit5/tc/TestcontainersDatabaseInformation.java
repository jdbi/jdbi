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
package org.jdbi.testing.junit5.tc;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.BiFunction;

import org.jdbi.meta.Alpha;
import org.jdbi.meta.Beta;
import org.testcontainers.containers.JdbcDatabaseContainer;

import static java.lang.String.format;

/**
 * Describes the parameters needed to create a new test-specific database or schema to isolate a test. Testcontainers supports many different databases and the
 * Jdbi specific extension requires parameterization.
 * <p></p>
 * The <a href="https://testcontainers.org/">Testcontainers</a> project provides a convenient way to spin up dependencies as containers and offers a Java API to
 * access these instances. Jdbi can use these instances for testing and offers the same functionality as the builtin support for PostgreSQL, H2 and Sqlite.
 * <p></p>
 * As every database engine is slightly different and supports different features, there is some "glue" needed, which is specific to the testcontainer class.
 * Out of the box, Jdbi works with the testcontainer support for MySQL, MariaDB, TiDB, PostgreSQL (inclusive PostGIS), CockroachDB, YugabyteDB, ClickhouseDB,
 * Oracle XE, Oracle Free, TrinoDB and MSSQL Server.
 * <br>
 * Any of the testcontainer instances for these databases, can be used with {@link JdbiTestcontainersExtension#instance(JdbcDatabaseContainer)} to create a
 * JUnit 5 {@link org.junit.jupiter.api.extension.Extension} instance that will manage database and schema instances for tests.
 * <p></p>
 * If a testcontainer class is not supported by Jdbi or the current Jdbi behavior does not match what is required for a test (e.g. schema or user creation), a
 * custom {@link TestcontainersDatabaseInformation} instance can be created using the
 * {@link TestcontainersDatabaseInformation#of(String, String, String, BiFunction)} method.
 * <br>
 * This instance can be passed into the {@link JdbiTestcontainersExtension#instance(TestcontainersDatabaseInformation, JdbcDatabaseContainer)} method to create
 * a custom JUnit 5 {@link org.junit.jupiter.api.extension.Extension}.
 * <br>
 */
@Beta
public final class TestcontainersDatabaseInformation {

    private static final TestcontainersDatabaseInformation CLICKHOUSE =
        // clickhouse pre-0.8 uses the catalog name.
        // clickhouse post-0.8 uses the schema name.
        of(null, null, null, (catalogName, schemaName) -> format("CREATE DATABASE %s Engine = Memory", schemaName));

    private static final TestcontainersDatabaseInformation MYSQL =
        of("root", null, null, (catalogName, schemaName) -> format("CREATE DATABASE %s", catalogName));

    // Oracle is ... special. This works with the gvenzl images; YMMV.
    private static final TestcontainersDatabaseInformation ORACLE_XE =
        ofScript("system", null, null, (catalogName, schemaName) -> List.of(
            format("CREATE USER %s IDENTIFIED BY %s QUOTA UNLIMITED ON USERS", schemaName, schemaName),
            format("GRANT CREATE session TO %s", schemaName),
            format("GRANT CREATE table TO %s", schemaName),
            format("GRANT CREATE view TO %s", schemaName),
            format("GRANT CREATE any trigger TO %s", schemaName),
            format("GRANT CREATE any procedure TO %s", schemaName),
            format("GRANT CREATE sequence TO %s", schemaName),
            format("GRANT CREATE synonym TO %s", schemaName)));

    private static final TestcontainersDatabaseInformation POSTGRES =
        of(null, "test", null, (catalogName, schemaName) -> format("CREATE SCHEMA %s", schemaName));

    private static final TestcontainersDatabaseInformation TRINO =
        of(null, "memory", null, (catalogName, schemaName) -> format("CREATE SCHEMA %s", schemaName));

    private static final TestcontainersDatabaseInformation MSSQL =
        of("sa", null, null, (catalogName, schemaName) -> format("CREATE DATABASE %s", catalogName));

    private static final TestcontainersDatabaseInformation DB2 =
        of(null, "test", null, (catalogName, schemaName) -> format("CREATE SCHEMA \"%s\"", schemaName));

    private static final Map<String, TestcontainersDatabaseInformation> KNOWN_CONTAINERS;

    static {
        Map<String, TestcontainersDatabaseInformation> knownContainers = new HashMap<>();
        // mysql crowd
        knownContainers.put("org.testcontainers.containers.MySQLContainer", MYSQL);
        knownContainers.put("org.testcontainers.containers.MariaDBContainer", MYSQL);
        knownContainers.put("org.testcontainers.tidb.TiDBContainer", MYSQL);

        // postgres crowd
        knownContainers.put("org.testcontainers.containers.PostgreSQLContainer", POSTGRES);
        knownContainers.put("org.testcontainers.containers.CockroachContainer", POSTGRES);
        knownContainers.put("org.testcontainers.containers.YugabyteDBYSQLContainer", POSTGRES);

        // odd ones
        knownContainers.put("org.testcontainers.containers.ClickHouseContainer", CLICKHOUSE);
        knownContainers.put("org.testcontainers.clickhouse.ClickHouseContainer", CLICKHOUSE);
        knownContainers.put("org.testcontainers.containers.OracleContainer", ORACLE_XE);
        knownContainers.put("org.testcontainers.oracle.OracleContainer", ORACLE_XE);
        knownContainers.put("org.testcontainers.containers.TrinoContainer", TRINO);
        knownContainers.put("org.testcontainers.containers.MSSQLServerContainer", MSSQL);
        knownContainers.put("org.testcontainers.containers.Db2Container", DB2);

        KNOWN_CONTAINERS = knownContainers;
    }

    /**
     * Returns a {@link TestcontainersDatabaseInformation} instance for a given container instance class.
     *
     * @param containerClazz A {@link JdbcDatabaseContainer} class object. Must not be null.
     * @return A {@link TestcontainersDatabaseInformation} instance describing how to use the database with the {@link JdbiTestcontainersExtension} or null if
     * the container class is unknown.
     */
    public static TestcontainersDatabaseInformation forTestcontainerClass(Class<? extends JdbcDatabaseContainer> containerClazz) {
        return KNOWN_CONTAINERS.get(containerClazz.getName());
    }

    private final String user;
    private final String catalog;
    private final String schema;
    private final BiFunction<String, String, List<String>> createStatement;

    private volatile int shutdownWaitTimeInSeconds = 10; // default shutdown wait time is 10 seconds

    /**
     * Creates a new database information instance that describes a database.
     *
     * @param user            Specify a user that can create a new schema or database. If this parameter is null, the testcontainer specific default user,
     *                        returned by {@link JdbcDatabaseContainer#getUsername()} is used.
     * @param catalog         Specify a catalog that should be used. This is for databases that do not support creating a new catalog or require a fixed catalog
     *                        for schema creation (e.g. Trino). If null, use a random catalog identifier.
     * @param schema          Specify a schema that should be used. This is for databases that do not support schemas but create a new database for each test.
     *                        If null, use a random schema identifier.
     * @param createStatement A {@link BiFunction} that returns a single statement to create a new database or schema for test isolation. The function is called
     *                        with the catalog and schema name and must return a single, valid SQL statement.
     * @return A {@link TestcontainersDatabaseInformation} object
     */
    public static TestcontainersDatabaseInformation of(String user, String catalog, String schema, BiFunction<String, String, String> createStatement) {
        return new TestcontainersDatabaseInformation(user, catalog, schema, wrapperFor(createStatement));
    }

    /**
     * Returns a {@link TestcontainersDatabaseInformation} object. It describes the credentials, schema and catalog to create a test databases.
     *
     * @param user            Specify a user that can create a new schema or database. If this parameter is null, the testcontainer specific default user,
     *                        returned by {@link JdbcDatabaseContainer#getUsername()} is used.
     * @param catalog         Specify a catalog that should be used. This is for databases that do not support creating a new catalog or require a fixed catalog
     *                        for schema creation (e.g. Trino). If null, use a random catalog identifier.
     * @param schema          Specify a schema that should be used. This is for databases that do not support schemas but create a new database for each test.
     *                        If null, use a random schema identifier.
     * @param createStatement A {@link BiFunction} that returns a single statement to create a new database or schema for test isolation. The function is called
     *                        with the catalog and schema name and must return a list of one or more valid SQL statements.
     * @return A {@link TestcontainersDatabaseInformation} object
     */
    public static TestcontainersDatabaseInformation ofScript(String user, String catalog, String schema,
        BiFunction<String, String, List<String>> createStatement) {
        return new TestcontainersDatabaseInformation(user, catalog, schema, createStatement);
    }

    private TestcontainersDatabaseInformation(String user, String catalog, String schema, BiFunction<String, String, List<String>> createStatement) {
        this.user = user;
        this.catalog = catalog;
        this.schema = schema;
        this.createStatement = createStatement;
    }

    /**
     * Returns the user for database creation.
     *
     * @return The user for database creation if configured or {@link Optional#empty()}
     */
    Optional<String> getUser() {
        return Optional.ofNullable(user);
    }

    /**
     * Returns the catalog for database creation.
     *
     * @return The catalog for database creation if configured or {@link Optional#empty()}
     */
    Optional<String> getCatalog() {
        return Optional.ofNullable(catalog);
    }

    /**
     * Returns the schema for database creation.
     *
     * @return The schema for database creation if configured or {@link Optional#empty()}
     */
    Optional<String> getSchema() {
        return Optional.ofNullable(schema);
    }

    TestcontainersDatabaseInformation forCatalogAndSchema(String catalog, String schema) {
        return new TestcontainersDatabaseInformation(user, catalog, schema, createStatement);
    }

    /**
     * Returns the database creation statements. These statements are executed to create a new
     * database or schema.
     *
     * @return A list of SQL statements to execute. May be empty but not null.
     */
    List<String> getCreationScript() {
        return this.createStatement.apply(
            getCatalog().orElseThrow(() -> new IllegalArgumentException("no catalog name present!")),
            getSchema().orElseThrow(() -> new IllegalArgumentException("no schema name present!")));
    }

    /**
     * Returns the time in seconds that a test will wait to shut down the Jdbi extension.
     *
     * @return The time to wait in seconds to shut down the Jdbi extension. 0 means wait infinitely until the extension has
     * been stopped. This can lead to infinite hangs in test cases and should be used with caution.
     * @since 3.45.0
     */
    @Alpha
    public int getShutdownWaitTimeInSeconds() {
        return shutdownWaitTimeInSeconds;
    }

    /**
     * Sets the wait time in seconds that a test will wait to shut down the Jdbi extension.
     *
     * @param shutdownWaitTimeInSeconds The wait time in seconds to shut down the Jdbi extension. Can be set to 0 which means that
     *                          it will wait infinitely until the extension has shut down. 0 may lead to infinite hangs in tests
     *                          and should be used with caution.
     * @since 3.45.0
     */
    @Alpha
    public void setShutdownWaitTimeInSeconds(int shutdownWaitTimeInSeconds) {
        if (shutdownWaitTimeInSeconds < 0) {
            throw new IllegalArgumentException("shutdownWaitTimeInSeconds must be >= 0!");
        }

        this.shutdownWaitTimeInSeconds = shutdownWaitTimeInSeconds;
    }

    @Override
    public String toString() {
        return new StringJoiner(".", "", "")
            .add(getCatalog().orElse("*"))
            .add(getSchema().orElse("*"))
            .toString();
    }

    private static BiFunction<String, String, List<String>> wrapperFor(BiFunction<String, String, String> createFunction) {
        return (catalogName, schemaName) -> Collections.singletonList(createFunction.apply(catalogName, schemaName));
    }
}
