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
package org.jdbi.v3;

import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.spi.JdbiPlugin;
import org.junit.rules.ExternalResource;

public class PGDatabaseRule extends ExternalResource
{
    private Jdbi dbi;
    private List<JdbiPlugin> plugins = new ArrayList<>();

    @Override
    protected void before() throws Throwable
    {
        assumeTrue(Boolean.parseBoolean(System.getenv("TRAVIS")));
        dbi = Jdbi.create("jdbc:postgresql:jdbi_test", "postgres", "");
        plugins.forEach(dbi::installPlugin);
    }

    @Override
    protected void after()
    {
        dbi = null;
    }

    public Jdbi getDbi()
    {
        return dbi;
    }

    public Handle openHandle()
    {
        return getDbi().open();
    }

    public PGDatabaseRule withPlugin(JdbiPlugin plugin) {
        plugins.add(plugin);
        return this;
    }
}
