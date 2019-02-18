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

import java.util.function.Consumer;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.testing.JdbiRule;

public class PostgresDbRule {
    private PostgresDbRule() {}

    public static JdbiRule rule() {
        return withPlugins(JdbiRule.embeddedPostgres());
    }

    public static JdbiRule rule(Consumer<EmbeddedPostgres.Builder> customizer) {
        return withPlugins(JdbiRule.embeddedPostgres(customizer));
    }

    private static JdbiRule withPlugins(JdbiRule rule) {
        return rule.withPlugin(new SqlObjectPlugin()).withPlugin(new PostgresPlugin());
    }
}
