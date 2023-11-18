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
package org.jdbi.v3.core.junit5;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleListener;
import org.jdbi.v3.core.statement.Cleanable;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementContextListener;

import static java.lang.String.format;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * A simple leak checker that tracks statement context and cleanable resource management.
 */
final class JdbiLeakChecker implements StatementContextListener, HandleListener {

    private final Map<StatementContext, RecordingContext<Cleanable>> contextElements = new HashMap<>();
    private final RecordingContext<Handle> handleTracker = new RecordingContext<>();

    @Override
    public void contextCreated(StatementContext statementContext) {
        synchronized (contextElements) {
            checkNotNull(statementContext, "statementContext is null!");

            assertThat(contextElements).withFailMessage("statement context has already been created").doesNotContainKey(statementContext);

            contextElements.putIfAbsent(statementContext, new RecordingContext<>());
        }
    }

    @Override
    public void contextCleaned(StatementContext statementContext) {
        checkNotNull(statementContext, "statementContext is null!");

        synchronized (contextElements) {
            assertThat(contextElements).withFailMessage("statement context is unknown").containsKey(statementContext);

            RecordingContext<Cleanable> context = contextElements.get(statementContext);
            Set<Cleanable> leakedCleanables = Sets.difference(context.objectAdded.keySet(), context.objectRemoved.keySet());
            if (!leakedCleanables.isEmpty()) {
                fail(format("Found %d cleanables that were not removed [%s]", leakedCleanables.size(), leakedCleanables));
            }
            context.reset();
        }
    }

    @Override
    public void cleanableAdded(StatementContext statementContext, Cleanable cleanable) {
        checkNotNull(statementContext, "statementContext is null!");
        checkNotNull(cleanable, "cleanable is null");

        synchronized (contextElements) {
            assertThat(contextElements).withFailMessage("statement context is unknown").containsKey(statementContext);

            RecordingContext<Cleanable> context = contextElements.get(statementContext);

            assertThat(context.objectAdded).withFailMessage("cleanable has already been added").doesNotContainKey(cleanable);
            assertThat(context.objectRemoved).withFailMessage("cleanable has already been removed").doesNotContainKey(cleanable);

            context.objectAdded.putIfAbsent(cleanable, Boolean.TRUE);
        }
    }

    @Override
    public void cleanableRemoved(StatementContext statementContext, Cleanable cleanable) {
        checkNotNull(statementContext, "statementContext is null!");
        checkNotNull(cleanable, "cleanable is null");

        synchronized (contextElements) {
            assertThat(contextElements).withFailMessage("statement context is unknown").containsKey(statementContext);

            RecordingContext<Cleanable> context = contextElements.get(statementContext);

            assertThat(context.objectAdded).withFailMessage("cleanable has not been added").containsKey(cleanable);
            assertThat(context.objectRemoved).withFailMessage("cleanable has already been removed").doesNotContainKey(cleanable);

            context.objectRemoved.putIfAbsent(cleanable, Boolean.TRUE);
        }
    }

    @Override
    public void handleCreated(Handle handle) {
        checkNotNull(handle, "handle is null");

        synchronized (handleTracker) {
            assertThat(handleTracker.objectAdded).withFailMessage("handle has already been added").doesNotContainKey(handle);
            assertThat(handleTracker.objectRemoved).withFailMessage("handle has already been removed").doesNotContainKey(handle);

            handleTracker.objectAdded.putIfAbsent(handle, Boolean.TRUE);
        }
    }

    @Override
    public void handleClosed(Handle handle) {
        checkNotNull(handle, "handle is null");

        synchronized (handleTracker) {
            assertThat(handleTracker.objectAdded).withFailMessage("handle has not been added").containsKey(handle);
            assertThat(handleTracker.objectRemoved).withFailMessage("handle has already been removed").doesNotContainKey(handle);

            handleTracker.objectRemoved.putIfAbsent(handle, Boolean.TRUE);
        }
    }

    public void checkForLeaks() {

        synchronized (handleTracker) {
            Set<Handle> leakedHandles = Sets.difference(handleTracker.objectAdded.keySet(), handleTracker.objectRemoved.keySet());

            if (!leakedHandles.isEmpty()) {
                fail(format("Found %d leaked handles.", leakedHandles.size()));
            }
        }

        synchronized (contextElements) {
            int leakedCleanablesCount = 0;

            for (RecordingContext<Cleanable> context : contextElements.values()) {
                Set<Cleanable> leakedCleanables = Sets.difference(context.objectAdded.keySet(), context.objectRemoved.keySet());
                if (!leakedCleanables.isEmpty()) {
                    leakedCleanablesCount += leakedCleanables.size();
                }
            }

            if (leakedCleanablesCount > 0) {
                fail(format("Found %d leaked cleanable objects in %d contexts", leakedCleanablesCount, contextElements.size()));
            }
        }
    }

    private static final class RecordingContext<T> {

        private final Map<T, Boolean> objectAdded = new HashMap<>();
        private final Map<T, Boolean> objectRemoved = new HashMap<>();

        public void reset() {
            objectAdded.clear();
            objectRemoved.clear();
        }
    }
}
