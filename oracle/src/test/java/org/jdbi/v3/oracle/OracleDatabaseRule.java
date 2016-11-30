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
package org.jdbi.v3.oracle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.ConnectionFactory;
import org.jdbi.v3.core.DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.junit.Assume;
import org.junit.rules.ExternalResource;

/**
 * Helper for a single, superuser privileged Oracle database.
 */
public class OracleDatabaseRule extends ExternalResource implements DatabaseRule {
    // Follow this guide to be able to run Oracle tests
    // https://dimitrisli.wordpress.com/2012/08/08/how-to-install-oracle-database-on-mac-os-any-version/

    // schema installed by default in Oracle DB Developer VM
    String uri = "jdbc:oracle:thin:@//127.0.0.1:1521/orcl";
    private Connection con;
    private Jdbi dbi;
    private Handle sharedHandle;
    private boolean installPlugins = false;
    private final List<JdbiPlugin> plugins = new ArrayList<>();

    @Override
    protected void before() throws Throwable {
        try {
            dbi = Jdbi.create(uri, "hr", "oracle");
        }
        catch (Exception e) {
            Assume.assumeNoException("Can't connect to Oracle database", e);
        }

        if (installPlugins) {
            dbi.installPlugins();
        }
        plugins.forEach(dbi::installPlugin);
        sharedHandle = dbi.open();
        sharedHandle.execute("create sequence something_id_sequence INCREMENT BY 1 START WITH 100");
        sharedHandle.execute("create table something (name varchar(200), id int, constraint something_id primary key (id))");
        con = sharedHandle.getConnection();
    }

    @Override
    protected void after() {
        try {
            sharedHandle.execute("drop table something");
            sharedHandle.execute("drop sequence something_id_sequence");

            con.close();
        } catch (SQLException e) {
            throw new AssertionError(e);
        }
    }

    public OracleDatabaseRule withPlugins() {
        installPlugins = true;
        return this;
    }

    public OracleDatabaseRule withPlugin(JdbiPlugin plugin) {
        plugins.add(plugin);
        return this;
    }

    public String getConnectionString() {
        return uri;
    }

    @Override
    public Jdbi getJdbi() {
        return dbi;
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
