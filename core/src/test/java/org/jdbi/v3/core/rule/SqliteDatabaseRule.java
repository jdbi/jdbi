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
package org.jdbi.v3.core.rule;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.ConnectionFactory;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.junit.rules.ExternalResource;

/**
 * Convenient database to test with if you specifically do not want to deal with data type and relation constraints: sqlite does not give a f***.
 *
 * Note that you will need to use the shared handle most of the time,
 * since sqlite memory instances are discarded when their last connections is closed.
 */
public class SqliteDatabaseRule extends ExternalResource implements DatabaseRule<SqliteDatabaseRule> {
    // no unique name needed: https://www.sqlite.org/inmemorydb.html
    private final String uri = "jdbc:sqlite::memory:";
    private Connection con;
    private Jdbi db;
    private Handle sharedHandle;
    private boolean installPlugins = false;
    private final List<JdbiPlugin> plugins = new ArrayList<>();

    @Override
    protected void before() {
        db = Jdbi.create(uri);
        if (installPlugins) {
            db.installPlugins();
        }
        plugins.forEach(db::installPlugin);
        sharedHandle = db.open();
        con = sharedHandle.getConnection();
    }

    @Override
    protected void after() {
        try {
            con.close();
        } catch (SQLException e) {
            throw new AssertionError(e);
        }
    }

    public SqliteDatabaseRule withPlugins() {
        installPlugins = true;
        return this;
    }

    @Override
    public SqliteDatabaseRule withPlugin(JdbiPlugin plugin) {
        plugins.add(plugin);
        return this;
    }

    public String getConnectionString() {
        return uri;
    }

    @Override
    public Jdbi getJdbi() {
        return db;
    }

    public Handle getSharedHandle() {
        return sharedHandle;
    }

    public Handle openHandle() {
        return getJdbi().open();
    }

    public ConnectionFactory getConnectionFactory() {
        return () -> DriverManager.getConnection(getConnectionString());
    }
}
