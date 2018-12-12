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
package org.jdbi.v3.core;

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.core.transaction.UnableToManipulateTransactionIsolationLevelException;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestHandle {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    @Test
    public void testInTransaction() {
        Handle h = dbRule.openHandle();

        String value = h.inTransaction(handle -> {
            handle.execute("insert into something (id, name) values (1, 'Brian')");
            return handle.createQuery("select name from something where id = 1").mapToBean(Something.class).findOnly().getName();
        });
        assertThat(value).isEqualTo("Brian");
    }

    @Test
    public void testSillyNumberOfCallbacks() throws Exception {
        try (Handle h = dbRule.openHandle()) {
            h.execute("insert into something (id, name) values (1, 'Keith')");
        }

        // strangely enough, the compiler can't infer this and thinks the throws is redundant
        String value = dbRule.getJdbi().<String, Exception>withHandle(handle ->
                handle.inTransaction(handle1 ->
                        handle1.createQuery("select name from something where id = 1").mapTo(String.class).findOnly()));

        assertThat(value).isEqualTo("Keith");
    }

    @SuppressWarnings("resource")
    @Test
    public void testIsClosed() {
        Handle h = dbRule.openHandle();
        assertThat(h.isClosed()).isFalse();
        h.close();
        assertThat(h.isClosed()).isTrue();
    }

    @Test
    public void testMrWinter() {
        final Handle h = dbRule.getSharedHandle();
        h.execute("CREATE TABLE \"\u2603\" (pk int primary key)");
    }

    @Test
    public void unknownTransactionLevelIsOk() {
        Handle h = dbRule.openHandle();

        assertThatThrownBy(() -> h.setTransactionIsolation(Integer.MIN_VALUE))
            .isInstanceOf(UnableToManipulateTransactionIsolationLevelException.class);

        assertThatCode(() -> h.setTransactionIsolation(TransactionIsolationLevel.UNKNOWN))
            .doesNotThrowAnyException();
    }
}
