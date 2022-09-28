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
package org.jdbi.v3.postgres;

import java.util.List;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.sqlobject.SingleValue;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestByteaArrays {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg).withPlugins(new SqlObjectPlugin(), new PostgresPlugin())
        .withInitializer((ds, h) -> h.useTransaction(th -> {
            th.execute("DROP TABLE IF EXISTS t");
            th.execute("CREATE TABLE t (b BYTEA[])");
        }));

    private Handle handle;

    @BeforeEach
    public void setUp() {
        handle = pgExtension.getSharedHandle();
    }

    @Test
    public void testByteaArrayMultiRows() {
        byte[] bytes1 = new byte[]{1, 2, 3};
        byte[] bytes2 = new byte[]{4, 8, 15, 16, 23, 42};

        int response = handle.createUpdate("INSERT INTO t (b) VALUES (:b)")
            .bindArray("b", bytes1, bytes2)
            .execute();

        assertThat(response).isOne();

        ByteaDao dao = handle.attach(ByteaDao.class);

        List<byte[]> result = dao.getAsMultipleRows();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(bytes1, bytes2);
    }

    @Test
    public void testByteaArraySingleRow() throws Exception {
        byte[] bytes1 = new byte[]{1, 2, 3};
        byte[] bytes2 = new byte[]{4, 8, 15, 16, 23, 42};
        byte[] bytes3 = new byte[]{10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0};
        byte[] bytes4 = new byte[]{1, 1, 2, 3, 5, 8, 13, 21};

        int response = handle.createUpdate("INSERT INTO t (b) VALUES (:b)")
            .bindArray("b", bytes1, bytes2)
            .execute();

        assertThat(response).isOne();

        response = handle.createUpdate("INSERT INTO t (b) VALUES (:b)")
            .bindArray("b", bytes3, bytes4)
            .execute();

        assertThat(response).isOne();

        ByteaDao dao = handle.attach(ByteaDao.class);

        List<byte[][]> result = dao.getAsSingleRows();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        byte[][] result0 = result.get(0);
        assertThat(result0.length).isEqualTo(2);
        assertThat(result0[0]).containsExactly(bytes1);
        assertThat(result0[1]).containsExactly(bytes2);

        byte[][] result1 = result.get(1);
        assertThat(result1.length).isEqualTo(2);
        assertThat(result1[0]).containsExactly(bytes3);
        assertThat(result1[1]).containsExactly(bytes4);
    }

    interface ByteaDao {
        @SqlQuery("SELECT b FROM t")
        @SingleValue
        List<byte[]> getAsMultipleRows();

        @SqlQuery("SELECT b FROM t")
        List<byte[][]> getAsSingleRows();
    }
}
