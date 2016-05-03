package org.jdbi.v3.pg;

import org.jdbi.v3.DBI;
import org.jdbi.v3.Handle;
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
    private DBI dbi;
    private Handle h;

    @Override
    public Statement apply(Statement base, Description description) {
        return pg.apply(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                dbi = DBI.create(pg.getEmbeddedPostgres().getDatabase("postgres", "postgres"))
                        .installPlugin(new PostgresJdbiPlugin())
                        .installPlugin(new SqlObjectPlugin());
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
