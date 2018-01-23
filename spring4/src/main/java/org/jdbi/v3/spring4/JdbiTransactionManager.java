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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * {@link org.springframework.transaction.PlatformTransactionManager}
 * implementation for Jdbi. Instead of Datasource/Connections this implementation
 * operated directly on Jdbi-native concepts such as {@link Jdbi} and {@link Handle}
 */
public class JdbiTransactionManager extends AbstractPlatformTransactionManager
        implements ResourceTransactionManager {

    // transient and serialUID to make findbugs happy
    private static final long serialVersionUID = 1L;
    private final transient Jdbi jdbi;

    public JdbiTransactionManager(Jdbi jdbi) {
        this.jdbi = jdbi;
        this.setNestedTransactionAllowed(true);
    }

    @Override
    protected Object doGetTransaction() throws TransactionException {
        final HandleHolder resource = getBoundResource();
        return new JdbiTransactionObject(resource);
    }

    private HandleHolder getBoundResource() {
        return (HandleHolder) TransactionSynchronizationManager.getResource(this.jdbi);
    }

    @Override
    protected boolean isExistingTransaction(Object transaction) throws TransactionException {
        final HandleHolder resource = getBoundResource();
        return resource != null && resource.isTransactionActive();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
        JdbiTransactionObject txObject = (JdbiTransactionObject) transaction;

        HandleHolder handleHolder = txObject.getHandleHolder();
        Handle handle = null;
        boolean openedNew = false;

        try {
            if (handleHolder == null) {
                Handle opened = jdbi.open();
                handleHolder = new HandleHolder(opened);
                openedNew = true;
            }

            handleHolder.setSynchronizedWithTransaction(true);
            handle = handleHolder.getHandle();
            TransactionIsolationLevel currentIsolationLevel = handle.getTransactionIsolationLevel();

            handle.begin();

            if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
                handleHolder.setTransactionLevelForRelease(currentIsolationLevel);
                handle.setTransactionIsolation(definition.getIsolationLevel());
            }

            int timeout = determineTimeout(definition);
            if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
                handleHolder.setTimeoutInSeconds(timeout);
            }

            txObject.setHandleHolder(handleHolder);

            if (openedNew) {
                TransactionSynchronizationManager.bindResource(jdbi, handleHolder);
            }
        } catch (Throwable ex) {
            if (openedNew && handle != null) {
                handle.close();
                txObject.setHandleHolder(null);
                if (TransactionSynchronizationManager.hasResource(jdbi)) {
                    TransactionSynchronizationManager.unbindResource(jdbi);
                }
            }
            throw new CannotCreateTransactionException("Could not open Jdbi Connection for transaction", ex);
        }
    }

    @Override
    protected void prepareSynchronization(DefaultTransactionStatus status, TransactionDefinition definition) {
        super.prepareSynchronization(status, definition);

        if (status.isNewSynchronization()) {
            HandleHolder holder = getBoundResource();
            TransactionSynchronizationManager.registerSynchronization(new HandleHolderSynchronization(holder, jdbi));
        }
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        JdbiTransactionObject txObject = (JdbiTransactionObject) status.getTransaction();
        Handle h = txObject.getHandleHolder().getHandle();

        try {
            h.commit();
        } catch (org.jdbi.v3.core.transaction.TransactionException ex) {
            throw new TransactionSystemException("Could not commit Jdbi transaction", ex);
        }
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        JdbiTransactionObject txObject = (JdbiTransactionObject) status.getTransaction();
        Handle h = txObject.getHandleHolder().getHandle();

        try {
            h.rollback();
        } catch (org.jdbi.v3.core.transaction.TransactionException ex) {
            throw new TransactionSystemException("Could not roll back Jdbi transaction", ex);
        }
    }

    @Override
    public Object getResourceFactory() {
        return jdbi;
    }

    @Override
    protected Object doSuspend(Object transaction) {
        JdbiTransactionObject txObject = (JdbiTransactionObject) transaction;
        final HandleHolder suspendedHolder = txObject.getHandleHolder();
        txObject.setHandleHolder(null);
        return suspendedHolder;
    }

    @Override
    protected void doResume(Object transaction, Object suspendedResources) {
        JdbiTransactionObject txObject = (JdbiTransactionObject) transaction;
        txObject.setHandleHolder((HandleHolder) suspendedResources);
    }
}
