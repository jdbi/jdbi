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
 * A reusable, immutable, thread-safe SQL statement definition. Built once from a {@link org.jdbi.core.Jdbi}
 * (see {@code Jdbi.buildStatementTemplate}) and executed many times by binding it to a {@link Handle} with
 * {@link #with(Handle)}.
 *
 * <p>Each execution binds its own parameters on the {@link Query} returned by {@link #with(Handle)} and then
 * chooses a terminal: a {@link ResultBearing} operation such as {@link Query#mapTo(Class)} to read rows, or
 * {@link Query#execute()} to run it as an update and get the modified-row count.
 */
public class StatementTemplate {
    final ConfigRegistry config;
    final String sql;

    // Rendered and parsed once at build time, then reused for every execution. Both are null when the
    // SQL depends on attributes supplied per execution (so it cannot be rendered once here); each
    // execution then renders and parses with its own defined attributes.
    final String renderedSql;
    final ParsedSql parsedSql;

    /**
     * Builds a template over the given SQL and configuration snapshot. The configuration is used
     * read-only and is not copied; callers pass a snapshot the template may retain (for example,
     * {@code jdbi.getConfig().createCopy()}).
     *
     * @param config the configuration snapshot to render, parse, and execute against
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
     * Binds this template to a handle for a single execution, returning a {@link Query} to bind parameters on
     * and run &mdash; as a query, or as an update via {@link Query#execute()}. The returned query is
     * thread-confined; obtain a fresh one for each execution.
     *
     * @param handle the handle to execute against
     * @return a fresh, thread-confined {@link Query}
     */
    public Query with(final Handle handle) {
        return new Query(handle, config, sql, renderedSql, parsedSql);
    }

    /**
     * Fixes this template's result type. The returned {@link MappedStatementTemplate} is also reusable and
     * thread-safe, and resolves the row mapper up front so its executions need not look one up.
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
