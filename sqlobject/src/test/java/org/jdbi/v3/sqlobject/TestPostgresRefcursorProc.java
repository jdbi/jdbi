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
import java.util.function.Function;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.OutParameters;
import org.jdbi.v3.sqlobject.customizer.OutParameter;
import org.jdbi.v3.sqlobject.statement.SqlCall;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestPostgresRefcursorProc {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg).withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @BeforeEach
    public void setUp() {
        handle = pgExtension.openHandle();
    }

    @AfterEach
    public void tearDown() {
        handle.close();
    }

    @Test
    public void multipleResultSetReturn() {
        handle.execute("create function gather_data (head out refcursor, tail out refcursor) "
            + "language plpgsql as $$ begin "
                + "open head for select 1 union select 2; "
                + "open tail for select 3 union select 4; end; $$");

        assertThat(handle.attach(Dao.class).gatherData(op -> {
            assertThat(op.getRowSet("head")
                     .mapTo(int.class)
                     .list())
                .contains(1, 2);
            assertThat(op.getRowSet("tail")
                    .mapTo(int.class)
                    .list())
                .contains(3, 4);
            return true;
        })).isTrue();
    }

    public interface Dao {
        @SqlCall("{call gather_data(:head, :tail)}")
        @OutParameter(name = "head", sqlType = Types.REF_CURSOR)
        @OutParameter(name = "tail", sqlType = Types.REF_CURSOR)
        @Transaction
        boolean gatherData(Function<OutParameters, Boolean> action);
    }
}
