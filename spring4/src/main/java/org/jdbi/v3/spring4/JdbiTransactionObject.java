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
package org.jdbi.v3.spring4;

import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.SavepointManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;

/**
 * savepoint capable jdbi transaction wrapper
 */
public class JdbiTransactionObject implements SavepointManager {

    private static final String JDBI_SAVEPOINT_BASENAME = "JDBI_SAVEPOINT_";

    private int nextSavePointNumber = 0;
    private HandleHolder handleHolder;

    public JdbiTransactionObject(HandleHolder handleHolder) {
        this.handleHolder = handleHolder;
    }

    @Override
    public Object createSavepoint() throws TransactionException {
        String savePointName = JDBI_SAVEPOINT_BASENAME + nextSavePointNumber++;
        try {
            handleHolder.getHandle().savepoint(savePointName);
        } catch (org.jdbi.v3.core.transaction.TransactionException ex) {
            throw new CannotCreateTransactionException("savepoint could not be created", ex);
        }

        return savePointName;
    }

    @Override
    public void rollbackToSavepoint(Object savepoint) throws TransactionException {
        if (!(savepoint instanceof String)) {
            throw new IllegalArgumentException("unhandleable savepoint: " + savepoint);
        }

        try {
            handleHolder.getHandle().rollbackToSavepoint((String) savepoint);
        } catch (org.jdbi.v3.core.transaction.TransactionException ex) {
            throw new TransactionSystemException("savepoint could be rolled back", ex);
        }
    }

    @Override
    public void releaseSavepoint(Object savepoint) throws TransactionException {
        if (!(savepoint instanceof String)) {
            throw new IllegalArgumentException("unhandleable savepoint: " + savepoint);
        }

        try {
            handleHolder.getHandle().release((String) savepoint);
        } catch (org.jdbi.v3.core.transaction.TransactionException ex) {
            // no-op since {@link org.springframework.transaction.support.AbstractTransactionStatus#rollbackToHeldSavepoint}
            // releases immediately after rollback -> jdbi throws exception
            // see org.springframework.jdbc.datasource.JdbcTransactionObjectSupport#releaseSavepoint
        }
    }

    public HandleHolder getHandleHolder() {
        return handleHolder;
    }

    public void setHandleHolder(HandleHolder handleHolder) {
        this.handleHolder = handleHolder;
    }
}
