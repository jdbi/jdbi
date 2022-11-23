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
package org.jdbi.v3.testing.junit5.internal;

import org.jdbi.v3.core.internal.UtilityClassException;

public final class FlywayVersionCheck {

    private FlywayVersionCheck() {
        throw new UtilityClassException();
    }

    /**
     * Turns out not every version of Flyway supports H2 migrations. Anything past 8.2.2 does.
     * Requires support from the build system, the POM in the testing project sets the flyway
     * version property as a system property for test execution.
     *
     * The default value below matches the current baseline in internal/build/pom.xml
     */
    public static boolean supportsH2Version2x() {
        String flywayVersion = System.getProperty("dep.flyway.version", "7.15.0");
        int[] supported = new int[]{8, 2, 2};  // 8.2.2 supports H2 v2
        String[] version = flywayVersion.split("[^\\d+]");

        int count = Math.min(supported.length, version.length);

        for (int i = 0; i < count; i++) {
            int v = Integer.parseInt(version[i]);
            if (v < supported[i]) {
                return false;
            } else if (v > supported[i]) {
                return true;
            }
        }

        return true;
    }
}
