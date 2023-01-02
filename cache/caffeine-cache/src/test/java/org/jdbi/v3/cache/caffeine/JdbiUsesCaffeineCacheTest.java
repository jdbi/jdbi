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
package org.jdbi.v3.cache.caffeine;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class JdbiUsesCaffeineCacheTest {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new CaffeineCachePlugin());

    @Test
    public void testUsingCaffeine() {

        Jdbi jdbi = h2Extension.getJdbi();

        SqlStatements sqlStatements = jdbi.getConfig(SqlStatements.class);
        assertThat((Object) sqlStatements.cacheStats()).isInstanceOf(CacheStats.class);
    }
}
