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

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.rule.PgDatabaseRule;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.junit.Rule;
import org.junit.Test;

public class TestReadOnly {
    // H2 does not support the readOnly connection hint.
    @Rule
    public PgDatabaseRule db = new PgDatabaseRule().withPlugin(new SqlObjectPlugin());

    @Test
    public void testHandleReadOnly() throws Exception {
        try (Handle h = db.openHandle()) {
            assertThat(h.isReadOnly()).isFalse();
            assertThat(h.getConnection().isReadOnly()).isFalse();

            h.setReadOnly(true);

            assertThat(h.isReadOnly()).isTrue();
            assertThat(h.getConnection().isReadOnly()).isTrue();
        }
    }

    @Test
    public void testSqlObjectReadOnly() throws Exception {
        try (Handle h = db.openHandle()) {
            RODao dao = h.attach(RODao.class);

            assertThat(h.isReadOnly()).isFalse();
            assertThat(dao.verifyReadOnly()).isTrue();
            assertThat(h.isReadOnly()).isFalse();
        }
    }

    @Test
    public void testReadOnlyInner() {
        try (Handle h = db.openHandle()) {
            RODao dao = h.attach(RODao.class);
            dao.writeTxn(() -> dao.readTxn(() -> {}));
        }
    }

    @Test(expected=TransactionException.class)
    public void testReadOnlyOuter() {
        try (Handle h = db.openHandle()) {
            RODao dao = h.attach(RODao.class);
            dao.readTxn(() -> dao.writeTxn(() -> {}));
        }
    }

    public interface RODao extends SqlObject {
        @Transaction(readOnly = true)
        default boolean verifyReadOnly() throws SQLException {
            final Handle h = getHandle();
            if (h.isReadOnly() != h.getConnection().isReadOnly()) {
                throw new AssertionError("didn't set");
            }
            return h.isReadOnly();
        }

        @Transaction(readOnly = false)
        default void writeTxn(Runnable r) {
            r.run();
        }

        @Transaction(readOnly = true)
        default void readTxn(Runnable r) {
            r.run();
        }
    }
}
