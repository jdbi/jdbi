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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestSerializableTransactionRunner
{
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    private Jdbi db;

    @Before
    public void setUp() throws Exception
    {
        db = Jdbi.create(dbRule.getConnectionFactory());
        db.setTransactionHandler(new SerializableTransactionRunner());
    }

    @Test
    public void testEventuallyFails() throws Exception
    {
        final AtomicInteger tries = new AtomicInteger(5);
        Handle handle = db.open();

        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> handle.inTransaction(TransactionIsolationLevel.SERIALIZABLE,
                        conn -> {
                            tries.decrementAndGet();
                            throw new SQLException("serialization", "40001");
                        }))
                .satisfies(e -> assertThat(e.getSQLState()).isEqualTo("40001"));
        assertThat(tries.get()).isEqualTo(0);
    }

    @Test
    public void testEventuallySucceeds() throws Exception
    {
        final AtomicInteger tries = new AtomicInteger(3);
        Handle handle = db.open();

        handle.inTransaction(TransactionIsolationLevel.SERIALIZABLE, conn -> {
            if (tries.decrementAndGet() == 0)
            {
                return null;
            }
            throw new SQLException("serialization", "40001");
        });

        assertThat(tries.get()).isZero();
    }
}
