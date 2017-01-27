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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.opentable.db.postgres.junit.EmbeddedPostgresRules;
import com.opentable.db.postgres.junit.SingleInstancePostgresRule;

/**
 * Helper for a single, superuser privileged Postgres database.
 */
public class PostgresDbRule implements TestRule {

    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private Jdbi db;
    private Handle h;

    @Override
    public Statement apply(Statement base, Description description) {
        return pg.apply(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                db = Jdbi.create(pg.getEmbeddedPostgres().getDatabase("postgres", "postgres"))
                        .installPlugin(new PostgresPlugin())
                        .installPlugin(new SqlObjectPlugin());
                h = db.open();

                try {
                    base.evaluate();
                } finally {
                    db = null;
                    h.close();
                    h = null;
                }
            }
        }, description);
    }

    public Jdbi getJdbi() {
        if (db == null) throw new AssertionError("Test not running");
        return db;
    }

    public Handle getSharedHandle() {
        if (h == null) throw new AssertionError("Test not running");
        return h;
    }
}
