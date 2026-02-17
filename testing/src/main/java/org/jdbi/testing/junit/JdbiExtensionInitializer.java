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
package org.jdbi.testing.junit;

import javax.sql.DataSource;

import org.jdbi.core.Handle;

/**
 * Initialize the data source before running a test. JdbiExtensionInitializer instances are often used to create the DDL schema for a test or preload data into
 * the data source.
 */
@FunctionalInterface
public interface JdbiExtensionInitializer {

    /**
     * Run initialization code before a test.
     *
     * @param ds     A reference to the managed {@link DataSource} which is controlled by a {@link JdbiExtension}.
     * @param handle The shared {@link Handle} which is returned from {@link JdbiExtension#getSharedHandle()}.
     * @see JdbiFlywayMigration
     */
    void initialize(DataSource ds, Handle handle);

    /**
     * Run cleanup code after a test.
     *
     * @param ds     A reference to the managed {@link DataSource} which is controlled by a {@link JdbiExtension}.
     * @param handle The shared {@link Handle} which is returned from {@link JdbiExtension#getSharedHandle()}.
     */
    default void cleanup(DataSource ds, Handle handle) {}
}
