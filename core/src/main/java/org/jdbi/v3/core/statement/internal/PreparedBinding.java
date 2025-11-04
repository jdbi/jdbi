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
package org.jdbi.v3.core.statement.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.jdbi.v3.core.argument.NamedArgumentFinder;
import org.jdbi.v3.core.argument.internal.NamedArgumentFinderFactory;
import org.jdbi.v3.core.internal.MemoizingSupplier;
import org.jdbi.v3.core.statement.Binding;
import org.jdbi.v3.core.statement.StatementContext;

public class PreparedBinding extends Binding {
    public PreparedBinding(StatementContext ctx) {
        super(ctx);
    }

    public final Map<NamedArgumentFinderFactory.PrepareKey, Object> prepareKeys = new HashMap<>();

    public final List<Supplier<NamedArgumentFinder>> backupArgumentFinders = new ArrayList<>();
    public final Supplier<List<NamedArgumentFinder>> realizedBackupArgumentFinders =
            MemoizingSupplier.of(() -> backupArgumentFinders.stream()
                .map(Supplier::get)
                .toList());

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && prepareKeys.isEmpty() && backupArgumentFinders.isEmpty();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("can't reset backup argument finders");
    }
}
