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
package org.jdbi.v3.core.test.junit5;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleListener;
import org.jdbi.v3.core.statement.Cleanable;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementContextListener;

import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;

/**
 * A simple leak checker that tracks statement context and cleanable resource management.
 */
final class JdbiLeakChecker implements StatementContextListener, HandleListener {

    private final ConcurrentMap<StatementContext, RecordingContext<Cleanable>> contextElements = new MapMaker().makeMap();
    private final RecordingContext<Handle> handleTracker = new RecordingContext<>();
    @Override
    public void contextCreated(StatementContext statementContext) {
        checkNotNull(statementContext, "statementContext is null!");

        assertThat(contextElements).as("statement context has already been created").doesNotContainKey(statementContext);

        contextElements.putIfAbsent(statementContext, new RecordingContext<>());
    }

    @Override
    public void contextCleaned(StatementContext statementContext) {
        checkNotNull(statementContext, "statementContext is null!");

        assertThat(contextElements).as("statement context is unknown").containsKey(statementContext);

        RecordingContext<Cleanable> context = contextElements.get(statementContext);
        Set<Cleanable> leakedCleanables = Sets.difference(context.objectAdded.keySet(), context.objectRemoved.keySet());
        if (!leakedCleanables.isEmpty()) {
            fail(format("Found %d cleanables that were not removed [%s]", leakedCleanables.size(), leakedCleanables));
        }
        context.reset();
    }

    @Override
    public void cleanableAdded(StatementContext statementContext, Cleanable cleanable) {
        checkNotNull(statementContext, "statementContext is null!");
        checkNotNull(cleanable, "cleanable is null");

        assertThat(contextElements).as("statement context is unknown").containsKey(statementContext);

        RecordingContext<Cleanable> context = contextElements.get(statementContext);

        assertThat(context.objectAdded).as("cleanable has already been added").doesNotContainKey(cleanable);
        assertThat(context.objectRemoved).as("cleanable has already been removed").doesNotContainKey(cleanable);

        context.objectAdded.putIfAbsent(cleanable, Boolean.TRUE);
    }

    @Override
    public void cleanableRemoved(StatementContext statementContext, Cleanable cleanable) {
        checkNotNull(statementContext, "statementContext is null!");
        checkNotNull(cleanable, "cleanable is null");

        assertThat(contextElements).as("statement context is unknown").containsKey(statementContext);

        RecordingContext<Cleanable> context = contextElements.get(statementContext);

        assertThat(context.objectAdded).as("cleanable has not been added").containsKey(cleanable);
        assertThat(context.objectRemoved).as("cleanable has already been removed").doesNotContainKey(cleanable);

        context.objectRemoved.putIfAbsent(cleanable, Boolean.TRUE);
    }

    @Override
    public void handleCreated(Handle handle) {
        checkNotNull(handle, "handle is null");

        assertThat(handleTracker.objectAdded).as("handle has already been added").doesNotContainKey(handle);
        assertThat(handleTracker.objectRemoved).as("handle has already been removed").doesNotContainKey(handle);

        handleTracker.objectAdded.putIfAbsent(handle, Boolean.TRUE);
    }

    @Override
    public void handleClosed(Handle handle) {
        checkNotNull(handle, "handle is null");

        assertThat(handleTracker.objectAdded).as("handle has not been added").containsKey(handle);
        assertThat(handleTracker.objectRemoved).as("handle has already been removed").doesNotContainKey(handle);

        handleTracker.objectRemoved.putIfAbsent(handle, Boolean.TRUE);
    }

    public void checkForLeaks() {

        Set<Handle> leakedHandles = Sets.difference(handleTracker.objectAdded.keySet(), handleTracker.objectRemoved.keySet());

        if (leakedHandles.size() > 0) {
            fail(format("Found %d leaked handles.", leakedHandles.size()));
        }

        int leakedCleanablesCount = 0;

        for (RecordingContext<Cleanable> context : contextElements.values()) {
            Set<Cleanable> leakedCleanables = Sets.difference(context.objectAdded.keySet(), context.objectRemoved.keySet());
            if (leakedCleanables.size() > 0) {
                leakedCleanablesCount += leakedCleanables.size();
            }
        }

        if (leakedCleanablesCount > 0) {
            fail(format("Found %d leaked cleanable objects in %d contexts", leakedCleanablesCount, contextElements.size()));
        }
    }

    private static final class RecordingContext<T> {

        private final ConcurrentMap<T, Boolean> objectAdded = new MapMaker().makeMap();
        private final ConcurrentMap<T, Boolean> objectRemoved = new MapMaker().makeMap();

        public void reset() {
            objectAdded.clear();
            objectRemoved.clear();
        }
    }
}
