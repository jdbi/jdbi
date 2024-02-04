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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.BiFunction;

import org.jdbi.v3.meta.Beta;
import org.testcontainers.containers.JdbcDatabaseContainer;

import static java.lang.String.format;

/**
 * Describes the parameters needed to create a new test-specific database or schema to isolate a test. Testcontainers supports many different databases and the
 * Jdbi specific extension requires parameterization.
 * <br>
 * Custom TestcontainersDatabaseInformation instances can be created e.g. for a specific local setup or a customer docker image.
 */
@Beta
public final class TestcontainersDatabaseInformation {

    private static final TestcontainersDatabaseInformation CLICKHOUSE =
        of(null, null, null, (catalogName, schemaName) -> format("CREATE DATABASE %s Engine = Memory", catalogName));

    private static final TestcontainersDatabaseInformation MYSQL =
        of("root", null, null, (catalogName, schemaName) -> format("CREATE DATABASE %s", catalogName));

    // Oracle is ... special. This works with the gvenzl images; YMMV.
    private static final TestcontainersDatabaseInformation ORACLE_XE =
        ofScript("system", null, null, (catalogName, schemaName) -> {
            List<String> script = new ArrayList<>();
            script.add(format("CREATE USER %s IDENTIFIED BY %s QUOTA UNLIMITED ON USERS", schemaName, schemaName));
            script.add(format("GRANT CREATE session TO %s", schemaName));
            script.add(format("GRANT CREATE table TO %s", schemaName));
            script.add(format("GRANT CREATE view TO %s", schemaName));
            script.add(format("GRANT CREATE any trigger TO %s", schemaName));
            script.add(format("GRANT CREATE any procedure TO %s", schemaName));
            script.add(format("GRANT CREATE sequence TO %s", schemaName));
            script.add(format("GRANT CREATE synonym TO %s", schemaName));
            return script;
        });

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
        knownContainers.put("org.testcontainers.containers.MySQLContainer", TestcontainersDatabaseInformation.MYSQL);
        knownContainers.put("org.testcontainers.containers.MariaDBContainer", TestcontainersDatabaseInformation.MYSQL);
        knownContainers.put("org.testcontainers.tidb.TiDBContainer", TestcontainersDatabaseInformation.MYSQL);

        // postgres crowd
        knownContainers.put("org.testcontainers.containers.PostgreSQLContainer", TestcontainersDatabaseInformation.POSTGRES);
        knownContainers.put("org.testcontainers.containers.CockroachContainer", TestcontainersDatabaseInformation.POSTGRES);
        knownContainers.put("org.testcontainers.containers.YugabyteDBYSQLContainer", TestcontainersDatabaseInformation.POSTGRES);

        // odd ones
        knownContainers.put("org.testcontainers.containers.ClickHouseContainer", TestcontainersDatabaseInformation.CLICKHOUSE);
        knownContainers.put("org.testcontainers.containers.OracleContainer", TestcontainersDatabaseInformation.ORACLE_XE);
        knownContainers.put("org.testcontainers.containers.TrinoContainer", TestcontainersDatabaseInformation.TRINO);
        knownContainers.put("org.testcontainers.containers.MSSQLServerContainer", TestcontainersDatabaseInformation.MSSQL);
        knownContainers.put("org.testcontainers.containers.Db2Container", TestcontainersDatabaseInformation.DB2);

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

    /**
     * Creates a new database information instance that describes a database.
     *
     * @param user            Specify a user that can create a new schema or database. If this parameter is null, the testcontainer specific default user is
     *                        used.
     * @param catalog         Specify a catalog that should be used. This is for databases that do not support creating a new catalog or require a fixed catalog
     *                        for schema creation (e.g. Trino). If null, use a random catalog identifier.
     * @param schema          Specify a schema that should be used. This is for databases that do not support schemas but create a new database for each test.
     *                        If null, use a random schema identifier.
     * @param createStatement Provides the statement to create a new database or schema for test isolation. It gets the selected catalog and schema name als
     *                        parameters and returns a valid SQL statement.
     * @return A {@link TestcontainersDatabaseInformation} object.
     */
    public static TestcontainersDatabaseInformation of(String user, String catalog, String schema, BiFunction<String, String, String> createStatement) {
        return new TestcontainersDatabaseInformation(user, catalog, schema, wrapperFor(createStatement));
    }

    /**
     * Creates a new database information instance that describes a database. This method is used for databases that require more than one statement to create a
     * new schema or database.
     *
     * @param user            Specify a user that can create a new schema or database. If this parameter is null, the testcontainer specific default user is
     *                        used.
     * @param catalog         Specify a catalog that should be used. This is for databases that do not support creating a new catalog or require a fixed catalog
     *                        for schema creation (e.g. Trino). If null, use a random catalog identifier.
     * @param schema          Specify a schema that should be used. This is for databases that do not support schemas but create a new database for each test.
     *                        If null, use a random schema identifier.
     * @param createStatement Provides the statement to create a new database or schema for test isolation. It gets the selected catalog and schema name als
     *                        parameters and returns a list of one or more valid SQL statements.
     * @return A {@link TestcontainersDatabaseInformation} object.
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

    Optional<String> getUser() {
        return Optional.ofNullable(user);
    }

    Optional<String> getCatalog() {
        return Optional.ofNullable(catalog);
    }

    Optional<String> getSchema() {
        return Optional.ofNullable(schema);
    }

    TestcontainersDatabaseInformation forCatalogAndSchema(String catalog, String schema) {
        return new TestcontainersDatabaseInformation(user, catalog, schema, createStatement);
    }

    List<String> getCreationScript() {
        return this.createStatement.apply(
            getCatalog().orElseThrow(() -> new IllegalArgumentException("no catalog name present!")),
            getSchema().orElseThrow(() -> new IllegalArgumentException("no schema name present!")));
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
