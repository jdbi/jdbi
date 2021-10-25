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

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("HideUtilityClassConstructor")
public class JdbiExtensionTest {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @Nested
    public class TestFlywayDefault {

        @RegisterExtension
        public JdbiExtension extension = JdbiExtension.postgres(pg).withInitializer(JdbiFlywayMigration.flywayMigration().withDefaultPath());

        @Test
        public void migrateWithFlywayDefaultLocation() {

            assertThat(extension.getSharedHandle()
                .select("select value from standard_migration_location")
                .mapTo(String.class)
                .one())
                .isEqualTo("inserted in migration script in the default location");
        }
    }

    @Nested
    public class TestFlywayCustomLocation {

        @RegisterExtension
        public JdbiExtension extension = JdbiExtension.postgres(pg)
            .withInitializer(JdbiFlywayMigration.flywayMigration().withPath("custom/migration/location"));

        @Test
        public void migrateWithFlywayCustomLocation() throws Throwable {
            assertThat(extension.getSharedHandle()
                .select("select value from custom_migration_location")
                .mapTo(String.class)
                .one())
                .isEqualTo("inserted in migration script in a custom location");
        }
    }

    @Nested
    public class TestFlywayMultipleLocations {

        @RegisterExtension
        public JdbiExtension extension = JdbiExtension.postgres(pg)
            .withInitializer(JdbiFlywayMigration.flywayMigration()
                .withPaths("custom/migration/location", "custom/migration/otherlocation"));

        @Test
        public void migrateWithFlywayMultipleLocations() {
            assertThat(
                extension.getSharedHandle()
                    .select("select value from custom_migration_location")
                    .mapTo(String.class)
                    .list())
                .containsOnly(
                    "inserted in migration script in a custom location",
                    "inserted in migration script in another custom location");
        }
    }
}
