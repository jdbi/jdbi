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

import java.sql.Types;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.OutParameters;
import org.jdbi.v3.sqlobject.customizer.OutParameter;
import org.jdbi.v3.sqlobject.statement.SqlCall;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestOutParameterAnnotation {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg)
        .withPlugin(new SqlObjectPlugin())
        .withInitializer((ds, h) -> {
            h.execute("CREATE FUNCTION set100(OUT outparam INT) AS $$ BEGIN outparam := 100; END; $$ LANGUAGE plpgsql");
            h.execute("CREATE FUNCTION swap(IN a INT, IN b INT, OUT c INT, OUT d INT) AS $$ BEGIN c := b; d := a; END; $$ LANGUAGE plpgsql");
        });

    private Jdbi db;

    @BeforeEach
    public void setUp() {
        db = pgExtension.getJdbi();
    }

    @Test
    public void testOutParameter() {
        MyDao myDao = db.onDemand(MyDao.class);

        OutParameters outParameters = myDao.callStoredProc();

        assertThat(outParameters.getInt("outparam")).isEqualTo(100);
    }

    @Test
    public void testMultipleOutParameters() {
        MyDao myDao = db.onDemand(MyDao.class);

        OutParameters outParameters = myDao.callMultipleOutParameters(1, 9);

        assertThat(outParameters.getInt("c")).isEqualTo(9);
        assertThat(outParameters.getInt("d")).isEqualTo(1);
    }

    public interface MyDao {
        @SqlCall("{call set100(:outparam)}")
        @OutParameter(name = "outparam", sqlType = Types.INTEGER)
        OutParameters callStoredProc();

        @SqlCall("{call swap(:a, :b, :c, :d)}")
        @OutParameter(name = "c", sqlType = Types.INTEGER)
        @OutParameter(name = "d", sqlType = Types.INTEGER)
        OutParameters callMultipleOutParameters(int a, int b);
    }
}
