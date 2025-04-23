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
package org.jdbi.v3.testing.junit5;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Handle;

/**
 * Use {@link Flyway} to create a database schema and/or preload a database instance.
 */
public final class JdbiFlywayMigration implements JdbiExtensionInitializer {

    private final List<String> schemas = new ArrayList<>();
    private final List<String> paths = new ArrayList<>();
    private volatile boolean cleanAfter = true;

    private volatile Flyway flyway;

    /**
     * Returns an instance of {@link JdbiFlywayMigration} which can be configured and used as a {@link JdbiExtensionInitializer}.
     *
     * <pre>{@code
     *     @RegisterExtension
     *     public JdbiExtension extension = JdbiExtension.h2()
     *             .withInitializer(JdbiFlywayMigration.flywayMigration().withDefaultPath());
     * }</pre>
     *
     * @return A {@link JdbiFlywayMigration} instance.
     */
    public static JdbiFlywayMigration flywayMigration() {
        return new JdbiFlywayMigration();
    }

    private JdbiFlywayMigration() {}

    /**
     * Use Default {@code db/migration} Flyway schema migration location. Migration scripts must be on the classpath.
     *
     * @return The instance itself for chaining method calls.
     */
    public JdbiFlywayMigration withDefaultPath() {
        this.paths.add("db/migration");
        return this;
    }

    /**
     * Add a custom flyway migration path.
     *
     * @param migrationPath The path to add. Must not be null.
     * @return The instance itself for chaining method calls.
     */
    public JdbiFlywayMigration withPath(final String migrationPath) {
        Objects.requireNonNull(migrationPath, "migrationPath is null");

        this.paths.add(migrationPath);
        return this;
    }

    /**
     * Add custom flyway migration paths.
     *
     * @param migrationPaths One or more paths to add. Must not be null.
     * @return The instance itself for chaining method calls.
     */
    public JdbiFlywayMigration withPaths(final String... migrationPaths) {
        Arrays.asList(migrationPaths).forEach(this::withPath);

        return this;
    }

    /**
     * Add flyway migration schema.
     *
     * @param schema A schema to add. Must not be null.
     * @return The instance itself for chaining method calls.
     */
    public JdbiFlywayMigration withSchema(final String schema) {
        Objects.requireNonNull(schema, "schema is null");

        this.schemas.add(schema);
        return this;
    }

    /**
     * Add flyway migration schemas.
     *
     * @param moreSchemas One or more schemas to add. Must not be null.
     * @return The instance itself for chaining method calls.
     */
    public JdbiFlywayMigration withSchemas(final String... moreSchemas) {
        Arrays.asList(moreSchemas).forEach(this::withSchema);

        return this;
    }

    /**
     * Will drop all objects in the configured schemas using Flyway after the test finishes.
     *
     * @return The instance itself for chaining method calls.
     * @deprecated The default changed to <tt>true</tt> so this call actually does nothing.
     */
    @Deprecated(since = "3.35.0", forRemoval = true)
    public JdbiFlywayMigration cleanAfter() {
        this.cleanAfter = true;
        return this;
    }

    /**
     * Drop all objects in the configured schemas using Flyway after the test finishes.
     *
     * @param cleanAfter Set to <tt>false</tt> to avoid cleaning of the schema.
     * @return The instance itself for chaining method calls.
     */
    public JdbiFlywayMigration cleanAfter(boolean cleanAfter) {
        this.cleanAfter = cleanAfter;
        return this;
    }

    @Override
    public void initialize(DataSource ds, Handle handle) {
        this.flyway = Flyway.configure()
            .dataSource(ds)
            .locations(paths.toArray(new String[0]))
            .schemas(schemas.toArray(new String[0]))
            .cleanDisabled(!this.cleanAfter)
            .load();

        this.flyway.migrate();
    }

    @Override
    public void cleanup(DataSource ds, Handle handle) {
        if (this.cleanAfter) {
            flyway.clean();
        }
    }
}
