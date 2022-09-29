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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.Nullable;
import org.jdbi.v3.core.argument.NamedArgumentFinder;
import org.jdbi.v3.core.argument.internal.NamedArgumentFinderFactory;
import org.jdbi.v3.core.argument.internal.TypedValue;
import org.jdbi.v3.core.internal.MemoizingSupplier;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;
import org.jdbi.v3.core.statement.Binding;
import org.jdbi.v3.core.statement.PreparedBatch;
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
                .collect(Collectors.toList()));

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && prepareKeys.isEmpty() && backupArgumentFinders.isEmpty();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("can't reset backup argument finders");
    }

    public Object computeCacheKey(Function<Object, QualifiedType<?>> typeOf) {
        List<Object> r = new ArrayList<>(4);
        r.add(positionals.entrySet().stream().map(e -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), typeOf.apply(e.getValue())))
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
        r.add(named.entrySet().stream().map(e -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), typeOf.apply(e.getValue())))
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
        r.add(prepareKeys.keySet());
        r.add(namedArgumentFinder.size()); // TODO use NamedArgumentFinder.getNames()
        return r;
    }
}
