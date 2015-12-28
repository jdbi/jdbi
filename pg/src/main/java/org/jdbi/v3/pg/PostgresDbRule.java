package org.jdbi.v3.pg;

import com.opentable.db.postgres.embedded.EmbeddedPostgreSQLRule;

import org.jdbi.v3.DBI;
import org.jdbi.v3.Handle;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Helper for a single, superuser privileged Postgres database.
 */
public class PostgresDbRule implements TestRule {

    public EmbeddedPostgreSQLRule pg = new EmbeddedPostgreSQLRule();
    private DBI dbi;
    private Handle h;

    @Override
    public Statement apply(Statement base, Description description) {
        return pg.apply(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                dbi = DBI.create(pg.getEmbeddedPostgreSQL().getDatabase("postgres", "postgres"))
                        .installPlugin(new PostgresJdbiPlugin());
                h = dbi.open();

                try {
                    base.evaluate();
                } finally {
                    dbi = null;
                    h.close();
                    h = null;
                }
            }
        }, description);
    }

    public DBI getDbi() {
        if (dbi == null) throw new AssertionError("Test not running");
        return dbi;
    }

    public Handle getSharedHandle() {
        if (h == null) throw new AssertionError("Test not running");
        return h;
    }
}
