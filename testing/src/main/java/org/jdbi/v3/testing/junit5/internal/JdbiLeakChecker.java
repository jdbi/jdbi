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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    private final Map<StatementContext, RecordingContext<Cleanable>> contextElements = new ConcurrentHashMap<>();
    private final RecordingContext<Handle> handleTracker = new RecordingContext<>();

    @Override
    public void contextCreated(StatementContext statementContext) {
        requireNonNull(statementContext, "statementContext is null!");

        assertFalse(contextElements.containsKey(statementContext), "statement context has already been created");

        contextElements.putIfAbsent(statementContext, new RecordingContext<>());
    }

    @Override
    public void contextCleaned(StatementContext statementContext) {
        requireNonNull(statementContext, "statementContext is null!");

        assertTrue(contextElements.containsKey(statementContext), "statement context is unknown");
        RecordingContext<Cleanable> context = contextElements.get(statementContext);

        Set<Cleanable> leakedCleanables = difference(context.objectAdded.keySet(), context.objectRemoved.keySet());
        if (!leakedCleanables.isEmpty()) {
            fail(format("Found %d cleanables that were not removed [%s]", leakedCleanables.size(), leakedCleanables));
        }
        context.reset();
    }

    @Override
    public void cleanableAdded(StatementContext statementContext, Cleanable cleanable) {
        requireNonNull(statementContext, "statementContext is null!");
        requireNonNull(cleanable, "cleanable is null");

        assertTrue(contextElements.containsKey(statementContext), "statement context is unknown");
        RecordingContext<Cleanable> context = contextElements.get(statementContext);

        assertFalse(context.objectAdded.containsKey(cleanable), "cleanable has already been added");
        assertFalse(context.objectRemoved.containsKey(cleanable), "cleanable has already been removed");

        context.objectAdded.putIfAbsent(cleanable, Boolean.TRUE);
    }

    @Override
    public void cleanableRemoved(StatementContext statementContext, Cleanable cleanable) {
        requireNonNull(statementContext, "statementContext is null!");
        requireNonNull(cleanable, "cleanable is null");

        assertTrue(contextElements.containsKey(statementContext), "statement context is unknown");
        RecordingContext<Cleanable> context = contextElements.get(statementContext);

        assertTrue(context.objectAdded.containsKey(cleanable), "cleanable has not been added");
        assertFalse(context.objectRemoved.containsKey(cleanable), "cleanable has already been removed");

        context.objectRemoved.putIfAbsent(cleanable, Boolean.TRUE);
    }

    @Override
    public void handleCreated(Handle handle) {
        requireNonNull(handle, "handle is null");

        assertFalse(handleTracker.objectAdded.containsKey(handle), "handle has already been added");
        assertFalse(handleTracker.objectRemoved.containsKey(handle), "handle has already been removed");

        handleTracker.objectAdded.putIfAbsent(handle, Boolean.TRUE);
    }

    @Override
    public void handleClosed(Handle handle) {
        requireNonNull(handle, "handle is null");

        assertTrue(handleTracker.objectAdded.containsKey(handle), "handle has not been added");
        assertFalse(handleTracker.objectRemoved.containsKey(handle), "handle has already been removed");

        handleTracker.objectRemoved.putIfAbsent(handle, Boolean.TRUE);
    }

    public void checkForLeaks() {
        Set<Handle> leakedHandles = difference(handleTracker.objectAdded.keySet(), handleTracker.objectRemoved.keySet());

        if (!leakedHandles.isEmpty()) {
            fail(format("Found %d leaked handles.", leakedHandles.size()));
        }

        int leakedCleanablesCount = 0;

        for (RecordingContext<Cleanable> context : contextElements.values()) {
            Set<Cleanable> leakedCleanables = difference(context.objectAdded.keySet(), context.objectRemoved.keySet());
            if (!leakedCleanables.isEmpty()) {
                leakedCleanablesCount += leakedCleanables.size();
            }
        }

        if (leakedCleanablesCount > 0) {
            fail(format("Found %d leaked cleanable objects in %d contexts", leakedCleanablesCount, contextElements.size()));
        }
    }

    private <T> Set<T> difference(Set<T> left, Set<T> right) {
        Set<T> result = new LinkedHashSet<>();
        for (T element : left) {
            if (right.contains(element)) { // present in both: ignore.
                continue;
            }
            result.add(element); // present only in left
        }

        return result;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return 11 * super.hashCode();
    }

    private static final class RecordingContext<T> {

        private final Map<T, Boolean> objectAdded = new ConcurrentHashMap<>();
        private final Map<T, Boolean> objectRemoved = new ConcurrentHashMap<>();

        public void reset() {
            objectAdded.clear();
            objectRemoved.clear();
        }
    }
}
