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

import org.jdbi.v3.core.JdbiException;

/**
 * Thrown when there's a problem manipulating the transaction isolation level.
 */
public class TransactionException extends JdbiException {
    private static final long serialVersionUID = 1L;

    public TransactionException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public TransactionException(Throwable cause) {
        super(cause);
    }

    public TransactionException(String msg) {
        super(msg);
    }
}
