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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class RollbackOnlyTransactionHandlerTest {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    @Test
    public void doubleInsert() {
        try (Handle h = h2Extension.openHandle()) {
            h.useTransaction(txn -> txn.execute("create table pk (id integer primary key)"));
        }

        h2Extension.getJdbi().setTransactionHandler(new RollbackOnlyTransactionHandler());

        try (Handle h = h2Extension.openHandle()) {
            h.useTransaction(txn ->
                assertThat(txn.execute("insert into pk values(1)")).isOne());
        }

        try (Handle h = h2Extension.openHandle()) {
            h.useTransaction(txn ->
                assertThat(txn.execute("insert into pk values(1)")).isOne());
        }
    }
}
