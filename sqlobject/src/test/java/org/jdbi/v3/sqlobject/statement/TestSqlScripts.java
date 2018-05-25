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
package org.jdbi.v3.sqlobject.statement;

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.locator.UseClasspathSqlLocator;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSqlScripts {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    @Test
    public void testCreateTable() {
        final Scriptacular scripts = db.getSharedHandle().attach(Scriptacular.class);
        scripts.createTable("cool_table");
        assertThat(scripts.doSomeUpdates()).containsExactly(3, 2);
        assertThat(scripts.externalScript()).containsExactly(0, 3, 1);
    }

    private interface Scriptacular {
        // tag::scripts[]
        @SqlScript("CREATE TABLE <name> (pk int primary key)")
        void createTable(@Define String name);

        @SqlScript("INSERT INTO cool_table VALUES (5), (6), (7)")
        @SqlScript("DELETE FROM cool_table WHERE pk > 5")
        int[] doSomeUpdates(); // returns [ 3, 2 ]

        @UseClasspathSqlLocator // load external SQL!
        @SqlScript // use the method name
        @SqlScript("secondScript") // or specify it yourself
        int[] externalScript();
        // end::scripts[]
    }
}
