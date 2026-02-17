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

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.SingleDatabaseBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class JdbiPostgresExtensionTest {

    @RegisterExtension
    public static EmbeddedPgExtension pg = SingleDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension postgres = JdbiExtension.postgres(pg);

    @Test
    public void testIsAlive() {
        Integer one = postgres.getJdbi().withHandle(h -> h.createQuery("select 1").mapTo(Integer.class).one());

        assertThat(one).isOne();
    }
}
