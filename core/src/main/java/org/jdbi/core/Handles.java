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
package org.jdbi.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jdbi.core.config.JdbiConfig;
import org.jdbi.core.internal.RegistrationLists;

/**
 * Configuration class for handles.
 * <p>
 * This configuration is immutable: the policy wither and the listener registration methods return a new
 * instance, leaving the receiver unchanged.
 */
public final class Handles implements JdbiConfig<Handles> {

    private final boolean forceEndTransactions;

    // Insertion order, no duplicates (a handle snapshots these at construction).
    private final List<HandleListener> handleListeners;

    public Handles() {
        this(true, List.of());
    }

    private Handles(boolean forceEndTransactions, List<HandleListener> handleListeners) {
        this.forceEndTransactions = forceEndTransactions;
        this.handleListeners = handleListeners;
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
     * Returns a copy of this configuration setting whether to enforce transaction termination discipline when a
     * {@link Handle} is closed.
     *
     * @param forceEndTransactions whether to enforce transaction termination
     *                             discipline.
     * @return the derived configuration
     */
    public Handles forceEndTransactions(boolean forceEndTransactions) {
        return new Handles(forceEndTransactions, handleListeners);
    }

    /**
     * Add a {@link HandleListener} which is called for specific events. Adding a listener will add
     * it to all Handles that are subsequently created (this call does not affect existing handles).
     *
     * @param handleListener A {@link HandleListener} object.
     *
     * @return a copy of this configuration with the listener registered
     */
    public Handles addListener(final HandleListener handleListener) {
        return new Handles(forceEndTransactions, RegistrationLists.appendDistinct(handleListeners, handleListener));
    }

    /**
     * Remove a {@link HandleListener}. Removing a listener will only affect Handles that are subsequently created, not existing handles.
     *
     * @param handleListener A {@link HandleListener} object.
     *
     * @return a copy of this configuration with the listener removed
     */
    public Handles removeListener(final HandleListener handleListener) {
        if (!handleListeners.contains(handleListener)) {
            return this;
        }
        final List<HandleListener> remaining = new ArrayList<>(handleListeners);
        remaining.remove(handleListener);
        return new Handles(forceEndTransactions, List.copyOf(remaining));
    }

    /**
     * Returns the collection of {@link HandleListener} objects. This set is immutable.
     *
     * @return A set of {@link HandleListener} objects. The set is never null, can be empty and is immutable.
     */
    public Set<HandleListener> getListeners() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(handleListeners));
    }

    CopyOnWriteArraySet<HandleListener> copyListeners() {
        return new CopyOnWriteArraySet<>(handleListeners);
    }

    @Override
    public Handles createCopy() {
        // Immutable: safe to share across registries.
        return this;
    }
}
