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

import java.lang.reflect.Type;

import com.google.errorprone.annotations.ThreadSafe;
import org.jdbi.core.Handle;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.generic.GenericType;
import org.jdbi.core.mapper.ColumnMapper;
import org.jdbi.core.mapper.MapperResolver;
import org.jdbi.core.mapper.NoSuchMapperException;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.mapper.SingleColumnMapper;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.core.result.ResultBearing;

/**
 * An immutable, reusable SQL statement definition, built once from a {@link org.jdbi.core.Jdbi} (see
 * {@code Jdbi.buildStatementTemplate}) and executed many times. It renders and parses its SQL once and reuses
 * that work on every execution, and is safe to share across threads.
 *
 * <p>To execute, bind the template to a {@link Handle}: {@link #with(Handle)} returns a {@link Query} &mdash; a
 * {@link ResultBearing} to map to rows, or run as an update via {@link Query#execute()} &mdash; {@link #call(Handle)}
 * returns a {@link Call}, and {@link #prepareBatch(Handle)} a {@link PreparedBatch}. Each returns a fresh,
 * single-use statement confined to the calling thread.
 */
@ThreadSafe
public class StatementTemplate {
    final ConfigRegistry config;
    final String sql;

    // Rendered and parsed once at build time, then reused for every execution. Both are null when the
    // SQL depends on attributes supplied per execution (so it cannot be rendered once here); each
    // execution then renders and parses with its own defined attributes.
    final String renderedSql;
    final ParsedSql parsedSql;

    /**
     * Builds a template over the given SQL, rendered and parsed once against the given configuration, which the
     * template retains and reads on every execution.
     *
     * @param config the configuration to render, parse, and execute against
     * @param sql    the SQL to render and parse once
     */
    public StatementTemplate(final ConfigRegistry config, final CharSequence sql) {
        this.config = config;
        this.sql = sql.toString();

        final SqlStatements stmtConfig = config.get(SqlStatements.class);
        String rendered;
        ParsedSql parsed;
        try {
            rendered = stmtConfig.preparedRender(this.sql, RenderContext.of(config));
            // The parser uses the context only for exception reporting; parsing depends solely on the SQL.
            parsed = stmtConfig.getSqlParser()
                .parse(rendered, StatementContext.create(config, null, StatementTemplate.class));
        } catch (final RuntimeException ignored) {
            // The SQL references attributes that are only defined per execution, so it cannot be
            // rendered once here; each execution renders and parses with its own defined attributes.
            rendered = null;
            parsed = null;
        }
        this.renderedSql = rendered;
        this.parsedSql = parsed;
    }

    /**
     * Binds this template to a handle, returning a {@link Query} to bind parameters on and run &mdash; as a query,
     * or as an update via {@link Query#execute()}.
     *
     * @param handle the handle to execute against
     * @return a fresh query for this execution
     */
    public Query with(final Handle handle) {
        return new Query(handle, config, sql, renderedSql, parsedSql);
    }

    /**
     * Binds this template to a handle as a stored-procedure {@link Call}: register out parameters, bind inputs,
     * then {@link Call#invoke()}.
     *
     * @param handle the handle to execute against
     * @return a fresh call for this execution
     */
    public Call call(final Handle handle) {
        return new Call(handle, config, sql, renderedSql, parsedSql);
    }

    /**
     * Binds this template to a handle as a {@link PreparedBatch}: add batches, then execute.
     *
     * @param handle the handle to execute against
     * @return a fresh prepared batch for this execution
     */
    public PreparedBatch prepareBatch(final Handle handle) {
        return new PreparedBatch(handle, config, sql, renderedSql, parsedSql);
    }

    /**
     * Fixes this template's result type, resolving the row mapper once at build time so executions need not
     * look one up.
     *
     * @param type the type to map result rows to
     * @param <T>  the type to map result rows to
     * @return a mapped template that produces {@code T}
     * @throws NoSuchMapperException if no row or column mapper is registered for the type
     */
    public <T> MappedStatementTemplate<T> mapTo(final Class<T> type) {
        return mapTo(QualifiedType.of(type));
    }

    /**
     * Fixes this template's result type. See {@link #mapTo(Class)}.
     *
     * @param type the type to map result rows to
     * @param <T>  the type to map result rows to
     * @return a mapped template that produces {@code T}
     * @throws NoSuchMapperException if no row or column mapper is registered for the type
     */
    public <T> MappedStatementTemplate<T> mapTo(final GenericType<T> type) {
        return mapTo(QualifiedType.of(type));
    }

    /**
     * Fixes this template's result type. See {@link #mapTo(Class)}.
     *
     * @param type the type to map result rows to
     * @return a mapped template that produces the given type
     * @throws NoSuchMapperException if no row or column mapper is registered for the type
     */
    public MappedStatementTemplate<?> mapTo(final Type type) {
        return mapTo(QualifiedType.of(type));
    }

    /**
     * Fixes this template's result type. See {@link #mapTo(Class)}.
     *
     * @param type the qualified type to map result rows to
     * @param <T>  the type to map result rows to
     * @return a mapped template that produces {@code T}
     * @throws NoSuchMapperException if no row or column mapper is registered for the type
     */
    public <T> MappedStatementTemplate<T> mapTo(final QualifiedType<T> type) {
        final RowMapper<T> mapper = MapperResolver.forRegistry(config).findMapper(type)
            .orElseThrow(() -> new NoSuchMapperException("No mapper registered for type " + type));
        return new MappedStatementTemplate<>(this, mapper);
    }

    /**
     * Fixes this template's result type to the given row mapper, without consulting the mapper
     * registry. Use this to bake in a mapper you already hold; use {@link #mapTo(Class)} to resolve
     * one from a registered type.
     *
     * @param mapper the row mapper each execution maps rows with
     * @param <T>    the type the mapper produces
     * @return a mapped template that produces {@code T}
     */
    public <T> MappedStatementTemplate<T> map(final RowMapper<T> mapper) {
        return new MappedStatementTemplate<>(this, mapper);
    }

    /**
     * Fixes this template's result type to the given column mapper, applied to the first column of
     * each row. See {@link #map(RowMapper)}.
     *
     * @param mapper the column mapper each execution maps the first column with
     * @param <T>    the type the mapper produces
     * @return a mapped template that produces {@code T}
     */
    public <T> MappedStatementTemplate<T> map(final ColumnMapper<T> mapper) {
        return map(new SingleColumnMapper<>(mapper));
    }
}
