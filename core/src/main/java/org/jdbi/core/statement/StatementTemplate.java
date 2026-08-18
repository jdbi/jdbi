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
import org.jdbi.core.config.ConfigView;
import org.jdbi.core.config.Configurable;
import org.jdbi.core.generic.GenericType;
import org.jdbi.core.mapper.ColumnMapper;
import org.jdbi.core.mapper.MapperResolver;
import org.jdbi.core.mapper.NoSuchMapperException;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.mapper.SingleColumnMapper;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.core.result.ResultBearing;
import org.jdbi.meta.Beta;

/**
 * An immutable, reusable SQL statement definition, built once from a {@link org.jdbi.core.Jdbi} (see
 * {@code Jdbi.buildStatementTemplate}) and executed many times. It renders and parses its SQL once and reuses
 * that work on every execution, and is safe to share across threads.
 *
 * <p>To execute, bind the template to a {@link Handle}: {@link #with(Handle)} returns a {@link Query} &mdash; a
 * {@link ResultBearing} to map to rows, or run as an update via {@link Query#execute()} &mdash; {@link #call(Handle)}
 * returns a {@link Call}, and {@link #prepareBatch(Handle)} a {@link PreparedBatch}. Each returns a fresh,
 * single-use statement confined to the calling thread.
 *
 * <p>{@code Jdbi.buildStatementTemplate(sql)} builds a template with the {@code Jdbi}'s configuration. To
 * configure a template on its own &mdash; register a mapper, define an attribute, set a template engine &mdash;
 * without deriving a whole new {@code Jdbi}, use the {@link Builder} returned by {@code Jdbi.statementTemplate(sql)}.
 */
@Beta
@ThreadSafe
public class StatementTemplate {
    final ConfigRegistry config;
    final String sql;

    // Rendered and parsed once at build time, then reused for every execution. Both are null when the
    // SQL depends on attributes supplied per execution (so it cannot be rendered once here); each
    // execution then renders and parses with its own defined attributes.
    final String renderedSql;
    final ParsedSql parsedSql;

    // Renders and parses the SQL once against the given (already-assembled) configuration, which the template
    // retains and reads on every execution. Package-private: a template is built through the Builder (see
    // Jdbi.statementTemplate / Jdbi.buildStatementTemplate), so no public entry point takes a raw ConfigRegistry.
    StatementTemplate(final ConfigRegistry config, final CharSequence sql) {
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
            // Rendering/parsing once here is a best-effort fast path. It most often fails because the SQL
            // references attributes that are only defined per execution; template engines signal that
            // differently (the default engine throws UnableToCreateStatementException, Freemarker an
            // IllegalStateException, and so on), so the whole family of runtime failures is treated the same:
            // fall back to rendering and parsing with the per-execution attributes on each execution. This does
            // not hide a genuine template or parser fault -- that fault simply re-surfaces when an execution
            // renders and parses the SQL for real.
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

    /**
     * Assembles a {@link StatementTemplate} whose configuration starts from a {@link org.jdbi.core.Jdbi} but is
     * tweaked for this template alone. Obtain one from {@code Jdbi.statementTemplate(sql)}, apply any
     * {@link Configurable} configuration (register mappers or arguments, set a template engine, define attributes),
     * then {@link #build()}.
     *
     * <p>The builder is {@link Configurable}, so it inherits the whole registration surface rather than repeating it.
     * Its configuration is a copy-on-write child of the {@code Jdbi}'s: an unconfigured builder shares the Jdbi's
     * warm resolvers, the first change forks a private copy, and the {@code Jdbi} is never affected.
     */
    @Beta
    public static final class Builder implements Configurable<Builder> {
        private final ConfigRegistry config;
        private final String sql;

        /**
         * Starts a template builder over a copy-on-write child of the given configuration.
         *
         * @param baseConfig the configuration to derive the template's configuration from (read-only)
         * @param sql        the SQL the built template renders and parses
         */
        public Builder(final ConfigView baseConfig, final CharSequence sql) {
            this.config = baseConfig.createChild();
            this.sql = sql.toString();
        }

        @Override
        public ConfigRegistry getConfig() {
            return config;
        }

        /**
         * Renders and parses the template against the configuration assembled so far. The returned template
         * captures that configuration; configuring the builder afterwards does not affect it.
         *
         * @return the reusable statement template
         */
        public StatementTemplate build() {
            return new StatementTemplate(config, sql);
        }
    }
}
