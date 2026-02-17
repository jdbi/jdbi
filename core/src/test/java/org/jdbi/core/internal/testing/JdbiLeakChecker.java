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
package org.jdbi.core.internal.testing;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;
import org.jdbi.core.Handle;
import org.jdbi.core.HandleListener;
import org.jdbi.core.statement.Cleanable;
import org.jdbi.core.statement.StatementContext;
import org.jdbi.core.statement.StatementContextListener;

import static java.lang.String.format;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * A simple leak checker that tracks statement context and cleanable resource management.
 */
final class JdbiLeakChecker implements StatementContextListener, HandleListener {

    private final ConcurrentMap<StatementContext, RecordingContext<Cleanable>> contextElements = new MapMaker().makeMap();
    private final RecordingContext<Handle> handleTracker = new RecordingContext<>();

    @Override
    public void contextCreated(StatementContext statementContext) {
        checkNotNull(statementContext, "statementContext is null!");

        assertThat(contextElements)
            .withFailMessage(() ->
                format("statement context %s has already been created by thread %s", statementContext, contextElements.get(statementContext)))
            .doesNotContainKey(statementContext);

        contextElements.putIfAbsent(statementContext, new RecordingContext<>());
    }

    @Override
    public void contextCleaned(StatementContext statementContext) {
        checkNotNull(statementContext, "statementContext is null!");

        assertThat(contextElements)
            .withFailMessage(() -> format("statement context %s is unknown", statementContext))
            .containsKey(statementContext);

        RecordingContext<Cleanable> context = contextElements.get(statementContext);

        Set<Cleanable> leakedCleanables = context.leakedElements();
        if (!leakedCleanables.isEmpty()) {
            fail(format("Found %d cleanables that were not removed [%s]", leakedCleanables.size(), leakedCleanables));
        }

        context.reset();
    }

    @Override
    public void cleanableAdded(StatementContext statementContext, Cleanable cleanable) {
        checkNotNull(statementContext, "statementContext is null!");
        checkNotNull(cleanable, "cleanable is null");

        assertThat(contextElements)
            .withFailMessage(() -> format("statement context %s is unknown", statementContext))
            .containsKey(statementContext);

        RecordingContext<Cleanable> context = contextElements.get(statementContext);

        assertThat(context.objectAdded)
            .withFailMessage(() -> format("cleanable %s has already been added by thread %s", cleanable, context.objectAdded.get(cleanable)))
            .doesNotContainKey(cleanable);
        assertThat(context.objectRemoved)
            .withFailMessage(() -> format("cleanable %s has already been removed by thread %s", cleanable, context.objectRemoved.get(cleanable)))
            .doesNotContainKey(cleanable);

        context.objectAdded.putIfAbsent(cleanable, getThreadName());
    }

    @Override
    public void cleanableRemoved(StatementContext statementContext, Cleanable cleanable) {
        checkNotNull(statementContext, "statementContext is null!");
        checkNotNull(cleanable, "cleanable is null");

        assertThat(contextElements)
            .withFailMessage(() -> format("statement context %s is unknown", statementContext))
            .containsKey(statementContext);

        RecordingContext<Cleanable> context = contextElements.get(statementContext);

        assertThat(context.objectAdded)
            .withFailMessage(() -> format("cleanable %s is unknown", cleanable))
            .containsKey(cleanable);
        assertThat(context.objectRemoved)
            .withFailMessage(() -> format("cleanable %s has already been removed by thread %s", cleanable, context.objectRemoved.get(cleanable)))
            .doesNotContainKey(cleanable);

        context.objectRemoved.putIfAbsent(cleanable, getThreadName());
    }

    @Override
    public void handleCreated(Handle handle) {
        checkNotNull(handle, "handle is null");

        assertThat(handleTracker.objectAdded)
            .withFailMessage(() -> format("handle %s has already been added by thread %s", handle, handleTracker.objectAdded.get(handle)))
            .doesNotContainKey(handle);

        assertThat(handleTracker.objectRemoved)
            .withFailMessage(() -> format("handle %s has already been removed by thread %s", handle, handleTracker.objectRemoved.get(handle)))
            .doesNotContainKey(handle);

        handleTracker.objectAdded.putIfAbsent(handle, getThreadName());
    }

    @Override
    public void handleClosed(Handle handle) {
        checkNotNull(handle, "handle is null");

        assertThat(handleTracker.objectAdded)
            .withFailMessage(() -> format("handle %s is unknown", handle))
            .containsKey(handle);

        assertThat(handleTracker.objectRemoved)
            .withFailMessage(() -> format("handle %s has already been removed by thread %s", handle, handleTracker.objectRemoved.get(handle)))
            .doesNotContainKey(handle);

        handleTracker.objectRemoved.putIfAbsent(handle, getThreadName());
    }

    public void checkForLeaks() {
        Set<Handle> leakedHandles = handleTracker.leakedElements();

        if (!leakedHandles.isEmpty()) {
            fail(format("Found %d leaked handles.", leakedHandles.size()));
        }

        int leakedCleanablesCount = 0;

        for (RecordingContext<Cleanable> context : contextElements.values()) {
            Set<Cleanable> leakedCleanables = context.leakedElements();
            if (!leakedCleanables.isEmpty()) {
                leakedCleanablesCount += leakedCleanables.size();
            }
        }

        if (leakedCleanablesCount > 0) {
            fail(format("Found %d leaked cleanable objects in %d contexts", leakedCleanablesCount, contextElements.size()));
        }
    }

    private static String getThreadName() {
        return Thread.currentThread().getName();
    }

    private static final class RecordingContext<T> {
        private final ConcurrentMap<T, String> objectAdded = new MapMaker().makeMap();
        private final ConcurrentMap<T, String> objectRemoved = new MapMaker().makeMap();

        public void reset() {
            objectAdded.clear();
            objectRemoved.clear();
        }

        public Set<T> leakedElements() {
            return Sets.difference(objectAdded.keySet(), objectRemoved.keySet());
        }

        @Override
        public String toString() {
            return "Context " + Integer.toHexString(System.identityHashCode(this));
        }
    }
}
