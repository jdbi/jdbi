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
package org.jdbi.v3.sqlobject;

import static org.junit.Assert.assertEquals;

import java.sql.Types;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.OutParameters;
import org.jdbi.v3.core.PgDatabaseRule;
import org.jdbi.v3.sqlobject.customizers.OutParameter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestOutParameterAnnotation {
    @Rule
    public PgDatabaseRule db = new PgDatabaseRule().withPlugin(new SqlObjectPlugin());

    private Jdbi dbi;

    @Before
    public void setUp() throws Exception {
        dbi = db.getJdbi();
        dbi.useHandle(h ->
            h.execute("CREATE FUNCTION set100(OUT outparam INT) AS $$ BEGIN outparam := 100; END; $$ LANGUAGE plpgsql"));
    }

    @Test
    public void testOutParameter() {
        MyDao myDao = dbi.onDemand(MyDao.class);

        OutParameters outParameters = myDao.callStoredProc();

        assertEquals(Integer.valueOf(100), outParameters.getInt("outparam"));
    }

    public interface MyDao {
        @SqlCall("{call set100(:outparam)}")
        @OutParameter(name="outparam", sqlType = Types.INTEGER)
        OutParameters callStoredProc();
    }
}
