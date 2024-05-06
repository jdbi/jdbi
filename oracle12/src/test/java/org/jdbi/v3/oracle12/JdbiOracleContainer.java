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
package org.jdbi.v3.oracle12;

import java.time.Duration;

import org.testcontainers.containers.OracleContainer;

public final class JdbiOracleContainer {
    private static final String CONTAINER_VERSION =
            "gvenzl/oracle-xe:" + System.getProperty("oracle.container.version", "slim-faststart");

    private JdbiOracleContainer() {}

    @SuppressWarnings("resource")
    public static OracleContainer create() {
        return new OracleContainer(CONTAINER_VERSION)
                .withStartupTimeout(Duration.ofMinutes(10));
    }
}
