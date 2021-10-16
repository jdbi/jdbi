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
package org.jdbi.v3.postgres;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.argument.TestInetAddressH2;
import org.jdbi.v3.core.junit5.PgDatabaseExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

public class TestInetAddressPg extends TestInetAddressH2 { { dbExtension = PgDatabaseExtension.instance(pg).withPlugin(new PostgresPlugin()); }

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @Override
    protected String getInetType() {
        return "INET";
    }
}
