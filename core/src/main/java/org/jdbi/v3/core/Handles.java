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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jdbi.v3.core.config.JdbiConfig;

/**
 * Configuration class for handles.
 */
public class Handles implements JdbiConfig<Handles> {

    private boolean forceEndTransactions = true;

    private final Set<HandleListener> handleListeners;

    public Handles() {
        handleListeners = new CopyOnWriteArraySet<>();
    }

    private Handles(Handles that) {
        this.forceEndTransactions = that.forceEndTransactions;
        this.handleListeners = new CopyOnWriteArraySet<>(that.handleListeners);
    }

    /**
     * Returns whether to enforce transaction termination discipline when a
     * {@link Handle} is closed. This check is enabled by default. If enabled,
     * and a handle is closed while a transaction is active (i.e. not committed
     * or rolled back), an exception will be thrown.
     * <br>
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
     * Add a {@link HandleListener} which is called for specific events. Adding a listener will add
     * it to all Handles that are subsequently created (this call does not affect existing handles).
     *
     * @param handleListener A {@link HandleListener} object.
     *
     * @return The Handles object itself.
     */
    public Handles addListener(final HandleListener handleListener) {
        this.handleListeners.add(handleListener);
        return this;
    }

    /**
     * Remove a {@link HandleListener}. Removing a listener will only affect Handles that are subsequently created, not existing handles.
     *
     * @param handleListener A {@link HandleListener} object.
     *
     * @return The Handles object itself.
     */
    public Handles removeListener(final HandleListener handleListener) {
        this.handleListeners.remove(handleListener);
        return this;
    }

    /**
     * Returns the collection of {@link HandleListener} objects. This set is immutable.
     *
     * @return A set of {@link HandleListener} objects. The set is never null, can be empty and is immutable.
     */
    public Set<HandleListener> getListeners() {
        return Collections.unmodifiableSet(handleListeners);
    }

    CopyOnWriteArraySet<HandleListener> copyListeners() {
        return new CopyOnWriteArraySet<>(handleListeners);
    }

    @Override
    public Handles createCopy() {
        return new Handles(this);
    }
}
