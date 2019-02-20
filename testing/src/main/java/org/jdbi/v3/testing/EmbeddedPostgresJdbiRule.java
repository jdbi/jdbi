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

import java.util.function.Consumer;

import javax.sql.DataSource;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import com.opentable.db.postgres.junit.EmbeddedPostgresRules;
import com.opentable.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

class EmbeddedPostgresJdbiRule extends JdbiRule {
    private final SingleInstancePostgresRule embeddedPg;

    EmbeddedPostgresJdbiRule() {
        embeddedPg = EmbeddedPostgresRules.singleInstance();
    }

    EmbeddedPostgresJdbiRule(Consumer<EmbeddedPostgres.Builder> customizer) {
        embeddedPg = EmbeddedPostgresRules.singleInstance().customize(customizer);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return embeddedPg.apply(super.apply(base, description), description);
    }

    @Override
    protected DataSource createDataSource() {
        return embeddedPg.getEmbeddedPostgres().getPostgresDatabase();
    }
}
