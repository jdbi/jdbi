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
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Rule;
import org.junit.Test;

public class RollbackOnlyTransactionHandlerTest {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    @Test
    public void doubleInsert() {
        try (Handle h = dbRule.openHandle()) {
            h.useTransaction(txn ->
                    txn.execute("create table pk (id integer primary key)"));
        }

        dbRule.getJdbi().setTransactionHandler(new RollbackOnlyTransactionHandler());

        try (Handle h = dbRule.openHandle()) {
            h.useTransaction(txn ->
                    txn.execute("insert into pk values(1)"));
        }

        try (Handle h = dbRule.openHandle()) {
            h.useTransaction(txn ->
                    txn.execute("insert into pk values(1)"));
        }
    }
}
