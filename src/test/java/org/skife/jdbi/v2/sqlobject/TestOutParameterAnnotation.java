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
package org.skife.jdbi.v2.sqlobject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.sql.Types;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.OutParameters;
import org.skife.jdbi.v2.sqlobject.customizers.OutParameter;
import org.skife.jdbi.v2.tweak.HandleCallback;

public class TestOutParameterAnnotation {
    @BeforeClass
    public static void isPostgresInstalled() {
        assumeTrue(Boolean.parseBoolean(System.getenv("TRAVIS")));
    }

    private DBI dbi;

    @Before
    public void setUp() throws Exception {
        dbi = new DBI("jdbc:postgresql:jdbi_test", "postgres", "");
        dbi.withHandle(new HandleCallback<Object>() {
            @Override
            public Object withHandle(Handle handle) throws Exception
            {
                handle.execute("CREATE FUNCTION set100(OUT outparam INT) AS $$ BEGIN outparam \\:= 100; END; $$ LANGUAGE plpgsql");
                return null;
            }
        });
    }

    @Test
    public void testOutParameter() {
        MyDao myDao = dbi.onDemand(MyDao.class);

        OutParameters outParameters = myDao.callStoredProc();

        assertEquals(Integer.valueOf(100), outParameters.getInt("outparam"));
    }

    public interface MyDao{
        @SqlCall("{call set100(:outparam)}")
        @OutParameter(name="outparam", sqlType = Types.INTEGER)
        OutParameters callStoredProc();
    }
}
