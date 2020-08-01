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

import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.meta.Beta;

/**
 * Configuration class for handles.
 */
public class Handles implements JdbiConfig<Handles> {
    private boolean forceEndTransactions = true;
    private boolean checkTransactionUsage = true;

    public Handles() {}

    private Handles(Handles that) {
        this.forceEndTransactions = that.forceEndTransactions;
        this.checkTransactionUsage = that.checkTransactionUsage;
    }

    /**
     * Returns whether to enforce transaction termination discipline when a
     * {@link Handle} is closed. This check is enabled by default. If enabled,
     * and a handle is closed while a transaction is active (i.e. not committed
     * or rolled back), an exception will be thrown.
     *
     * This check does not apply to handles allocated with a connection that
     * already has an open transaction.
     *
     * @return whether to enforce transaction termination discipline when a
     * {@link Handle} is closed.
     */
    public boolean isForceEndTransactions() {
        return forceEndTransactions;
    }

    /**
     * Sets whether to enforce transaction termination discipline when a
     * {@link Handle} is closed.
     *
     * @param forceEndTransactions whether to enforce transaction termination
     *                             discipline.
     */
    public void setForceEndTransactions(boolean forceEndTransactions) {
        this.forceEndTransactions = forceEndTransactions;
    }

    /**
     * @return should Jdbi do some basic transaction usage checks
     * @see #setCheckTransactionUsage(boolean)
     */
    @Beta
    public boolean isCheckTransactionUsage() {
        return checkTransactionUsage;
    }

    /**
     * Transactions as implemented by JDBC do not directly map to Jdbi transactions.
     * For example, nested {@code @Transaction}s are not a driver feature.
     * Jdbi does some sanity checks.  Sometimes, if you're programming the driver
     * directly, that gets in the way. This turns them off...
     * @param checkTransactionUsage
     */
    @Beta
    public void setCheckTransactionUsage(boolean checkTransactionUsage) {
        this.checkTransactionUsage = checkTransactionUsage;
    }

    @Override
    public Handles createCopy() {
        return new Handles(this);
    }
}
