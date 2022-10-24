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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;
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
class JdbiLeakChecker implements StatementContextListener {

    private final ConcurrentMap<StatementContext, Deque<CleanableContext>> contextElements = new MapMaker().weakKeys().makeMap();

    @Override
    public void contextCreated(StatementContext statementContext) {
        checkNotNull(statementContext, "statementContext is null!");

        assertThat(contextElements).as("statement context has already been created").doesNotContainKey(statementContext);

        contextElements.putIfAbsent(statementContext, new ArrayDeque<>());
        contextElements.get(statementContext).push(new CleanableContext());
    }

    @Override
    public void contextCleaned(StatementContext statementContext) {
        checkNotNull(statementContext, "statementContext is null!");

        assertThat(contextElements).as("statement context is unknown").containsKey(statementContext);
        contextElements.get(statementContext).push(new CleanableContext());
    }

    @Override
    public Optional<Cleanable> cleanableAdded(StatementContext statementContext, Cleanable cleanable) {
        checkNotNull(statementContext, "statementContext is null!");
        checkNotNull(cleanable, "cleanable is null");

        assertThat(contextElements).as("statement context is unknown").containsKey(statementContext);

        CleanableContext context = contextElements.get(statementContext).peek();

        assertThat(context.objectAdded).as("cleanable has already been added").doesNotContainKey(cleanable);
        assertThat(context.objectRemoved).as("cleanable has already been removed").doesNotContainKey(cleanable);

        context.objectAdded.putIfAbsent(cleanable, Boolean.TRUE);
        return Optional.empty();
    }

    @Override
    public Optional<Cleanable> cleanableRemoved(StatementContext statementContext, Cleanable cleanable) {
        checkNotNull(statementContext, "statementContext is null!");
        checkNotNull(cleanable, "cleanable is null");

        assertThat(contextElements).as("statement context is unknown").containsKey(statementContext);

        CleanableContext context = contextElements.get(statementContext).peek();

        assertThat(context.objectAdded).as("cleanable has not been added").containsKey(cleanable);
        assertThat(context.objectRemoved).as("cleanable has already been removed").doesNotContainKey(cleanable);

        context.objectRemoved.putIfAbsent(cleanable, Boolean.TRUE);
        return Optional.empty();
    }

    public void assertState() {
//         AtomicInteger leakedContextCount = new AtomicInteger();
        AtomicInteger leakedCleanablesCount = new AtomicInteger();

        contextElements.forEach((statementContext, stack) -> {

            for (CleanableContext context : stack) {
                Set<Cleanable> leakedCleanables = Sets.difference(context.objectAdded.keySet(), context.objectRemoved.keySet());
                if (leakedCleanables.size() > 0) {
                    leakedCleanablesCount.addAndGet(leakedCleanables.size());
                }
            }
        });

//        if (strictStatementContextChecking && leakedContextCount.get() > 0) {
//            fail(format("Found %d leaked statement context objects.", leakedContextCount.get()));
//        }
//
        if (leakedCleanablesCount.get() > 0) {
            fail(format("Found %d leaked cleanable objects in %d contexts", leakedCleanablesCount.get(), contextElements.size()));
        }
    }

    private static class CleanableContext {

        private final ConcurrentMap<Cleanable, Boolean> objectAdded = new MapMaker().weakKeys().makeMap();
        private final ConcurrentMap<Cleanable, Boolean> objectRemoved = new MapMaker().weakKeys().makeMap();
    }
}
