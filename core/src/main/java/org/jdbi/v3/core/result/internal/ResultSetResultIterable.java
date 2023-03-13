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
package org.jdbi.v3.core.result.internal;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collector;

import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.result.ResultIterator;
import org.jdbi.v3.core.result.ResultSetException;
import org.jdbi.v3.core.result.UnableToProduceResultException;
import org.jdbi.v3.core.statement.StatementContext;

public class ResultSetResultIterable<T> implements ResultIterable<T> {
    private final RowMapper<T> mapper;
    private final StatementContext ctx;
    private final Supplier<ResultSet> resultSetSupplier;
    private final Type elementType;

    public ResultSetResultIterable(
            RowMapper<T> mapper,
            StatementContext ctx,
            Supplier<ResultSet> resultSetSupplier,
            Type elementType) {
        this.mapper = mapper;
        this.ctx = ctx;
        this.resultSetSupplier = resultSetSupplier;
        this.elementType = GenericTypes.box(elementType);
    }

    @Override
    public ResultIterator<T> iterator() {
        try {
            ResultSet resultSet = resultSetSupplier.get();
            ctx.addCleanable(resultSet::close);

            return new ResultSetResultIterator<>(resultSet, mapper, ctx);
        } catch (final SQLException e) {
            throw new ResultSetException("Unable to iterate result set", e, ctx);
        }
    }

    @Override
    public List<T> list() {
        if (elementType == null) {
            return ResultIterable.super.list();
        }
        return collectInto(GenericTypes.parameterizeClass(List.class, elementType));
    }

    @Override
    public Set<T> set() {
        if (elementType == null) {
            return ResultIterable.super.set();
        }
        return collectInto(GenericTypes.parameterizeClass(Set.class, elementType));
    }

    @SuppressWarnings("unchecked")
    private <R extends Collection<? super T>> R collectInto(Type containerType) {
        return collect((Collector<? super T, ?, R>) ctx.findCollectorFor(containerType)
                .orElseThrow(() -> new UnableToProduceResultException("Could not find collector for " + containerType)));
    }
}
