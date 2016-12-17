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

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.oracle.OracleReturning.returnParameters;
import static org.jdbi.v3.oracle.OracleReturning.returningDml;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.Rule;
import org.junit.Test;

import oracle.jdbc.OracleTypes;

public class TestOracleReturning {

    @Rule
    public OracleDatabaseRule db = new OracleDatabaseRule().withPlugin(new SqlObjectPlugin());

    @Test
    public void testReturningDml() {
        Handle h = db.getSharedHandle();

        List<Integer> ids = h.createUpdate("insert into something(id, name) values (17, 'Brian') returning id into ?")
                .addCustomizer(returnParameters().register(1, OracleTypes.INTEGER))
                .execute(returningDml())
                .mapTo(int.class)
                .list();

        assertThat(ids).containsExactly(17);
    }
}
