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
package org.jdbi.core.statement;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.result.ResultIterable;

/**
 * A single, thread-confined execution of a {@link MappedQueryTemplate} against a specific handle. Bind
 * parameters (and optionally override defined attributes or set query customizers), then obtain the
 * results with {@link #results()}. This mirrors {@link QueryTemplateBinding}, except the result mapper is
 * fixed by the owning {@link MappedQueryTemplate}, so {@link #results()} takes no type argument and
 * performs no mapper lookup.
 *
 * <p>All binding, defining, and customizing operations are delegated to an underlying
 * {@link QueryTemplateBinding}, so they behave identically to a plain template execution; only the
 * terminal result step differs.
 *
 * @param <T> the result type this query maps rows to
 */
public class BoundMappedQuery<T> implements QueryCustomizerMixin<BoundMappedQuery<T>> {
    private final QueryTemplateBinding binding;
    private final RowMapper<T> mapper;

    BoundMappedQuery(final QueryTemplateBinding binding, final RowMapper<T> mapper) {
        this.binding = binding;
        this.mapper = mapper;
    }

    @Override
    public Binding getBinding() {
        return binding.getBinding();
    }

    @Override
    public ConfigRegistry getConfig() {
        return binding.getConfig();
    }

    @Override
    public StatementContext getContext() {
        return binding.getContext();
    }

    @Override
    public BoundMappedQuery<T> define(final String key, final Object value) {
        binding.define(key, value);
        return this;
    }

    @Override
    public BoundMappedQuery<T> addCustomizer(final StatementCustomizer customizer) {
        binding.addCustomizer(customizer);
        return this;
    }

    @Override
    public BoundMappedQuery<T> setQueryTimeout(final int seconds) {
        binding.setQueryTimeout(seconds);
        return this;
    }

    @Override
    public BoundMappedQuery<T> attachToHandleForCleanup() {
        binding.attachToHandleForCleanup();
        return this;
    }

    @Override
    @SafeVarargs
    public final <E> BoundMappedQuery<T> bindArray(final int pos, final E... array) {
        return QueryCustomizerMixin.super.bindArray(pos, array);
    }

    @Override
    @SafeVarargs
    public final <E> BoundMappedQuery<T> bindArray(final String name, final E... array) {
        return QueryCustomizerMixin.super.bindArray(name, array);
    }

    /**
     * The mapped analogue of the terminal {@code mapTo(type)} step: the result type is already fixed by the
     * owning {@link MappedQueryTemplate}, so this takes no type argument and performs no mapper lookup. It
     * runs the query and returns its rows as a {@link ResultIterable} of that type, using the mapper resolved
     * once at build time (compare {@link org.jdbi.core.result.ResultBearing#mapTo(Class)} on the unmapped path).
     *
     * @return a {@link ResultIterable} of the template's result type
     */
    public ResultIterable<T> results() {
        return binding.map(mapper);
    }
}
