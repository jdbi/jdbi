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
package org.jdbi.v3.core.transaction;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestSerializableTransactionRunner
{
    private static final int RETRIES = 5;

    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    private Jdbi db;

    @Before
    public void setUp() throws Exception
    {
        db = Jdbi.create(dbRule.getConnectionFactory());
        db.setTransactionHandler(new SerializableTransactionRunner());
        db.configure(SerializableTransactionRunner.Configuration.class, config -> config.setMaxRetries(RETRIES));
    }

    @Test
    public void testEventuallyFails() {
        final AtomicInteger attempts = new AtomicInteger(0);
        Handle handle = db.open();

        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> handle.inTransaction(TransactionIsolationLevel.SERIALIZABLE,
                        conn -> {
                            attempts.incrementAndGet();
                            throw new SQLException("serialization", "40001");
                        }))
                .satisfies(e -> assertThat(e.getSQLState()).isEqualTo("40001"))
                .satisfies(e -> assertThat(e.getSuppressed()).hasSize(RETRIES));
        assertThat(attempts.get()).isEqualTo(1 + RETRIES);
    }

    @Test
    public void testEventuallySucceeds() throws Exception
    {
        final AtomicInteger remaining = new AtomicInteger(RETRIES / 2);
        Handle handle = db.open();

        handle.inTransaction(TransactionIsolationLevel.SERIALIZABLE, conn -> {
            if (remaining.decrementAndGet() == 0)
            {
                return null;
            }
            throw new SQLException("serialization", "40001");
        });

        assertThat(remaining.get()).isZero();
    }

    @Test
    public void testNonsenseRetryCount() {
        assertThatThrownBy(() -> db.configure(SerializableTransactionRunner.Configuration.class, config -> config.setMaxRetries(-1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Set a number >= 0");
    }
}
