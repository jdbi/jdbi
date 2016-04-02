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
package org.jdbi.v3.tweak.transactions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.jdbi.v3.Handle;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class TestLocalTransactionHandler {
    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    Handle h;

    @Test
    public void testRollbackThrow() throws Exception {
        RuntimeException outer = new RuntimeException("Transaction throws!");
        RuntimeException inner = new RuntimeException("Rollback throws!");

        Mockito.when(h.rollback()).thenThrow(inner);

        try {
            new LocalTransactionHandler().inTransaction(h,
                (h, txn) -> { throw outer; });
        } catch (RuntimeException e) {
            assertSame(outer, e);
            assertEquals(1, e.getSuppressed().length);
            assertSame(inner, e.getSuppressed()[0]);
        }
    }
}
