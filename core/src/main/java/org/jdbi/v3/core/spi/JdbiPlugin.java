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
package org.jdbi.v3.core.spi;

import java.sql.Connection;
import java.sql.SQLException;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

/**
 * A plugin is given an opportunity to customize instances of various {@code Jdbi}
 * types before they are returned from their factories.
 */
public interface JdbiPlugin {
    /**
     * Configure customizations global to any object managed by this Jdbi.
     * This method is invoked immediately when the plugin is installed.
     * @param jdbi the jdbi to customize
     * @throws SQLException something went wrong with the database
     */
    default void customizeJdbi(Jdbi jdbi) throws SQLException {}

    /**
     * Configure customizations for a new Handle instance.
     * @param handle the handle just created
     * @return the transformed handle
     * @throws SQLException something went wrong with the database
     */
    default Handle customizeHandle(Handle handle) throws SQLException {
        return handle;
    }

    /**
     * Configure customizations for a newly acquired Connection.
     * @param conn the connection Jdbi acquired
     * @return the transformed connection to use
     * @throws SQLException something went wrong with the database
     */
    default Connection customizeConnection(Connection conn) throws SQLException {
        return conn;
    }
}
