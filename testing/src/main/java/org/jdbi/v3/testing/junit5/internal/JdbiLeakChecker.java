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
package org.jdbi.v3.testing.junit5.internal;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleListener;
import org.jdbi.v3.core.statement.Cleanable;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementContextListener;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * A simple leak checker that tracks statement context and cleanable resource management.
 */
public final class JdbiLeakChecker implements StatementContextListener, HandleListener {

    private final ConcurrentMap<StatementContext, RecordingContext<Cleanable>> contextElements = new ConcurrentHashMap<>();
    private final RecordingContext<Handle> handleTracker = new RecordingContext<>();

    @Override
    public void contextCreated(StatementContext statementContext) {
        requireNonNull(statementContext, "statementContext is null!");

        assertFalse(contextElements.containsKey(statementContext),
            () -> format("statement context %s has already been created by thread %s", statementContext, contextElements.get(statementContext)));

        contextElements.putIfAbsent(statementContext, new RecordingContext<>());
    }

    @Override
    public void contextCleaned(StatementContext statementContext) {
        requireNonNull(statementContext, "statementContext is null!");

        assertTrue(contextElements.containsKey(statementContext),
            () -> format("statement context %s is unknown", statementContext));

        RecordingContext<Cleanable> context = contextElements.get(statementContext);

        Set<Cleanable> leakedCleanables = context.leakedElements();
        if (!leakedCleanables.isEmpty()) {
            fail(format("Found %d cleanables that were not removed [%s]", leakedCleanables.size(), leakedCleanables));
        }

        context.reset();
    }

    @Override
    public void cleanableAdded(StatementContext statementContext, Cleanable cleanable) {
        requireNonNull(statementContext, "statementContext is null!");
        requireNonNull(cleanable, "cleanable is null");

        assertTrue(contextElements.containsKey(statementContext),
            () -> format("statement context %s is unknown", statementContext));

        RecordingContext<Cleanable> context = contextElements.get(statementContext);

        assertFalse(context.objectAdded.containsKey(cleanable),
            () -> format("cleanable %s has already been added by thread %s", cleanable, context.objectAdded.get(cleanable)));
        assertFalse(context.objectRemoved.containsKey(cleanable),
            () -> format("cleanable %s has already been removed by thread %s", cleanable, context.objectRemoved.get(cleanable)));

        context.objectAdded.putIfAbsent(cleanable, getThreadName());
    }

    @Override
    public void cleanableRemoved(StatementContext statementContext, Cleanable cleanable) {
        requireNonNull(statementContext, "statementContext is null!");
        requireNonNull(cleanable, "cleanable is null");

        assertTrue(contextElements.containsKey(statementContext),
            () -> format("statement context %s is unknown", statementContext));

        RecordingContext<Cleanable> context = contextElements.get(statementContext);

        assertTrue(context.objectAdded.containsKey(cleanable),
            () -> format("cleanable %s is unknown", cleanable));
        assertFalse(context.objectRemoved.containsKey(cleanable),
            () -> format("cleanable %s has already been removed by thread %s", cleanable, context.objectRemoved.get(cleanable)));

        context.objectRemoved.putIfAbsent(cleanable, getThreadName());
    }

    @Override
    public void handleCreated(Handle handle) {
        requireNonNull(handle, "handle is null");

        assertFalse(handleTracker.objectAdded.containsKey(handle),
            () -> format("handle %s has already been added by thread %s", handle, handleTracker.objectAdded.get(handle))
        );
        assertFalse(handleTracker.objectRemoved.containsKey(handle),
            () -> format("handle %s has already been removed by thread %s", handle, handleTracker.objectRemoved.get(handle))
        );

        handleTracker.objectAdded.putIfAbsent(handle, getThreadName());
    }

    @Override
    public void handleClosed(Handle handle) {
        requireNonNull(handle, "handle is null");

        assertTrue(handleTracker.objectAdded.containsKey(handle),
            () -> format("handle %s is unknown", handle));
        assertFalse(handleTracker.objectRemoved.containsKey(handle),
            () -> format("handle %s has already been removed by thread %s", handle, handleTracker.objectRemoved.get(handle)));

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

        private final ConcurrentMap<T, String> objectAdded = new ConcurrentHashMap<>();
        private final ConcurrentMap<T, String> objectRemoved = new ConcurrentHashMap<>();

        public void reset() {
            objectAdded.clear();
            objectRemoved.clear();
        }

        public Set<T> leakedElements() {
            Set<T> result = new LinkedHashSet<>();
            for (T element : objectAdded.keySet()) {
                if (objectRemoved.containsKey(element)) { // present in both: ignore.
                    continue;
                }
                result.add(element); // present only in left
            }

            return result;
        }
    }
}
