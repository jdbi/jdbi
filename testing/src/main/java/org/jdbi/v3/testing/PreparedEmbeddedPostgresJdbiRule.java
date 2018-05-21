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
package org.jdbi.v3.testing;

import com.opentable.db.postgres.embedded.FlywayPreparer;
import com.opentable.db.postgres.junit.EmbeddedPostgresRules;
import com.opentable.db.postgres.junit.PreparedDbRule;
import org.jdbi.v3.core.Jdbi;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

class PreparedEmbeddedPostgresJdbiRule extends JdbiRule {

    private final PreparedDbRule embeddedPreparedDbRule;

    PreparedEmbeddedPostgresJdbiRule(final String... migrationLocations) {
        String[] locations = migrationLocations != null && migrationLocations.length > 0
            ? migrationLocations
            : new String[]{"db/migration"};
        embeddedPreparedDbRule = EmbeddedPostgresRules.preparedDatabase(FlywayPreparer.forClasspathLocation(locations));
    }

    @Override
    protected Jdbi createJdbi() {
        return Jdbi.create(embeddedPreparedDbRule.getTestDatabase());
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return embeddedPreparedDbRule.apply(super.apply(base, description), description);
    }
}
