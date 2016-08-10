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
package org.jdbi.v3.core;

import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.spi.JdbiPlugin;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.opentable.db.postgres.junit.EmbeddedPostgresRules;
import com.opentable.db.postgres.junit.PreparedDbRule;

public class PgDatabaseRule extends ExternalResource implements DatabaseRule
{
    private Jdbi jdbi;
    private final List<JdbiPlugin> plugins = new ArrayList<>();
    private JdbiPreparer preparer;
    private PreparedDbRule innerRule;

    @Override
    public Statement apply(Statement base, Description description) {
        if (preparer == null) {
            preparer = new JdbiPreparer.None();
        }
        innerRule = EmbeddedPostgresRules.preparedDatabase(preparer);
        return innerRule.apply(super.apply(base, description), description);
    }

    @Override
    protected void before() throws Throwable
    {
        jdbi = Jdbi.create(innerRule.getTestDatabase());
        plugins.forEach(jdbi::installPlugin);
    }

    @Override
    protected void after()
    {
        jdbi = null;
    }

    @Override
    public Jdbi getJdbi()
    {
        return jdbi;
    }

    public Handle openHandle()
    {
        return getJdbi().open();
    }

    public PgDatabaseRule withPlugin(JdbiPlugin plugin) {
        plugins.add(plugin);
        return this;
    }

    public PgDatabaseRule withPreparer(JdbiPreparer preparer) {
        this.preparer = preparer;
        return this;
    }
}
