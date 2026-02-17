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
package org.jdbi.sqlobject;

import java.sql.Types;
import java.util.function.Consumer;
import java.util.function.Function;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.statement.OutParameters;
import org.jdbi.core.statement.UnableToExecuteStatementException;
import org.jdbi.sqlobject.customizer.OutParameter;
import org.jdbi.sqlobject.statement.SqlCall;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    Jdbi db;
    Handle handle;

    @BeforeEach
    void setUp() {
        db = pgExtension.getJdbi();
        handle = pgExtension.getSharedHandle();
    }

    @Test
    void testOutParameterReturnExtension() {
        db.useExtension(MyDao.class, myDao -> {
            OutParameters outParameters = myDao.callStoredProc();
            assertThat(outParameters.getInt("outparam")).isEqualTo(100);
        });
    }

    @Test
    void testOutParameterReturnOnDemand() {
        MyDao myDao = db.onDemand(MyDao.class);
        OutParameters outParameters = myDao.callStoredProc();
        assertThatThrownBy(() -> {
            assertThat(outParameters.getInt("outparam")).isEqualTo(100);
        }).isInstanceOf(UnableToExecuteStatementException.class);
    }

    @Test
    void testOutParameterReturnHandleAttach() {
        MyDao myDao = handle.attach(MyDao.class);
        OutParameters outParameters = myDao.callStoredProc();
        assertThat(outParameters.getInt("outparam")).isEqualTo(100);
    }

    @Test
    void testUseOutParameterExtension() {
        db.useExtension(MyDao.class, myDao ->
            myDao.useStoredProc(outParameters -> assertThat(outParameters.getInt("outparam")).isEqualTo(100)));
    }

    @Test
    void testUseOutParameterOnDemand() {
        MyDao myDao = db.onDemand(MyDao.class);
        myDao.useStoredProc(outParameters -> assertThat(outParameters.getInt("outparam")).isEqualTo(100));
    }

    @Test
    void testUseOutParameterHandleAttach() {
        MyDao myDao = handle.attach(MyDao.class);
        myDao.useStoredProc(outParameters -> assertThat(outParameters.getInt("outparam")).isEqualTo(100));
    }

    @Test
    void testWithOutParameterExtension() {
        db.useExtension(MyDao.class, myDao ->
            assertThat(myDao.withStoredProc((Function<OutParameters, Integer>) outParameters -> outParameters.getInt("outparam")))
                .isEqualTo(100));
    }

    @Test
    void testWithOutParameterOnDemand() {
        MyDao myDao = db.onDemand(MyDao.class);
        assertThat(myDao.withStoredProc((Function<OutParameters, Integer>) outParameters -> outParameters.getInt("outparam")))
            .isEqualTo(100);
    }

    @Test
    void testWithOutParameterHandleAttach() {
        MyDao myDao = handle.attach(MyDao.class);
        assertThat(myDao.withStoredProc((Function<OutParameters, Integer>) outParameters -> outParameters.getInt("outparam")))
            .isEqualTo(100);
    }

    @Test
    void testReturnMultipleOutParametersExtension() {
        db.useExtension(MyDao.class, myDao -> {
            OutParameters outParameters = myDao.callMultipleOutParameters(1, 9);
            assertThat(outParameters.getInt("c")).isEqualTo(9);
            assertThat(outParameters.getInt("d")).isOne();
        });
    }

    @Test
    void testReturnMultipleOutParametersOnDemandFails() {
        MyDao myDao = db.onDemand(MyDao.class);
        OutParameters outParameters = myDao.callMultipleOutParameters(1, 9);

        assertThatThrownBy(() -> {
            assertThat(outParameters.getInt("c")).isEqualTo(9);
            assertThat(outParameters.getInt("d")).isOne();
        }).isInstanceOf(UnableToExecuteStatementException.class);
    }

    @Test
    void testReturnMultipleOutParametersHandleAttach() {
        MyDao myDao = handle.attach(MyDao.class);
        OutParameters outParameters = myDao.callMultipleOutParameters(1, 9);
        assertThat(outParameters.getInt("c")).isEqualTo(9);
        assertThat(outParameters.getInt("d")).isOne();
    }

    @Test
    void testUseMultipleOutParametersExtension() {
        db.useExtension(MyDao.class, myDao -> {
            myDao.useMultipleOutParameters(1, 9, outParameters -> {
                assertThat(outParameters.getInt("c")).isEqualTo(9);
                assertThat(outParameters.getInt("d")).isOne();
            });
        });
    }

    @Test
    void testUseMultipleOutParametersOnDemand() {
        MyDao myDao = db.onDemand(MyDao.class);
        myDao.useMultipleOutParameters(1, 9, outParameters -> {
            assertThat(outParameters.getInt("c")).isEqualTo(9);
            assertThat(outParameters.getInt("d")).isOne();
        });
    }

    @Test
    void testUseMultipleOutParametersHandleAttach() {
        MyDao myDao = handle.attach(MyDao.class);
        myDao.useMultipleOutParameters(1, 9, outParameters -> {
            assertThat(outParameters.getInt("c")).isEqualTo(9);
            assertThat(outParameters.getInt("d")).isOne();
        });
    }

    public interface MyDao {
        @SqlCall("{call set100(:outparam)}")
        @OutParameter(name = "outparam", sqlType = Types.INTEGER)
        OutParameters callStoredProc();

        @SqlCall("{call set100(:outparam)}")
        @OutParameter(name = "outparam", sqlType = Types.INTEGER)
        void useStoredProc(Consumer<OutParameters> consumer);

        @SqlCall("{call set100(:outparam)}")
        @OutParameter(name = "outparam", sqlType = Types.INTEGER)
        <T> T withStoredProc(Function<OutParameters, T> transformer);

        @SqlCall("{call swap(:a, :b, :c, :d)}")
        @OutParameter(name = "c", sqlType = Types.INTEGER)
        @OutParameter(name = "d", sqlType = Types.INTEGER)
        OutParameters callMultipleOutParameters(int a, int b);

        @SqlCall("{call swap(:a, :b, :c, :d)}")
        @OutParameter(name = "c", sqlType = Types.INTEGER)
        @OutParameter(name = "d", sqlType = Types.INTEGER)
        void useMultipleOutParameters(int a, int b, Consumer<OutParameters> consumer);
    }
}
