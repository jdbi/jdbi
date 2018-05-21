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
        embeddedPreparedDbRule = EmbeddedPostgresRules.preparedDatabase(FlywayPreparer.forClasspathLocation(migrationLocations));
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
