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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Consumer;

import org.jdbi.core.Handle;
import org.jdbi.core.config.ConfigView;
import org.jdbi.core.generic.GenericType;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.statement.internal.JfrSupport;
import org.jdbi.core.statement.internal.OptionalEvent;

/**
 * This class provides the common functions between <code>Query</code> and
 * <code>Update</code>. It defines most of the argument binding functions
 * used by its subclasses.
 */
public abstract class SqlStatement<This extends SqlStatement<This>> extends BaseStatement<This> implements Customizable<This> {
    private final String sql;

    // Non-null only for a statement built from a reusable template: the SQL was rendered and parsed once at
    // build time. parseSql() reuses them unless this execution supplied per-execution defines (which change
    // the rendering); a one-shot statement renders and parses lazily on every execution.
    private final String cachedRenderedSql;
    private final ParsedSql cachedParsedSql;

    PreparedStatement stmt;

    SqlStatement(final Handle handle,
                 final CharSequence sql) {
        this(handle, handle.getConfig(), sql, null, null);
    }

    SqlStatement(final Handle handle,
                 final ConfigView parentConfig,
                 final CharSequence sql,
                 final String cachedRenderedSql,
                 final ParsedSql cachedParsedSql) {
        super(handle, parentConfig);

        this.sql = sql.toString();
        this.cachedRenderedSql = cachedRenderedSql;
        this.cachedParsedSql = cachedParsedSql;

        getContext()
            .setConnection(handle.getConnection())
            .setRawSql(this.sql);
    }

    @Override
    public Binding getBinding() {
        return getContext().getBinding();
    }

    /**
     * Returns the un-translated SQL used to create this statement.
     *
     * @return the un-translated SQL used to create this statement.
     */
    protected String getSql() {
        return sql;
    }

    /**
     * Set the query timeout, in seconds, on the prepared statement.
     *
     * @param seconds number of seconds before timing out
     *
     * @return the same Query instance
     */
    @Override
    public This setQueryTimeout(final int seconds) {
        return addCustomizer(StatementCustomizers.statementTimeout(seconds));
    }

    /**
     * Transfer ownership of the handle to the statement: when the statement is closed,
     * commit the handle's transaction (if one exists) and close the handle.
     * @return this
     */
    public This cleanupHandleCommit() {
        return cleanupHandle(Handle::commit);
    }

    /**
     * When the statement is closed, roll it back then close the owning Handle.
     * @return this
     */
    public This cleanupHandleRollback() {
        return cleanupHandle(Handle::rollback);
    }

    private This cleanupHandle(final Consumer<Handle> action) {
        getContext().addCleanable(() -> {
            try (Handle handle = getHandle()) {
                if (handle.isInTransaction()) {
                    action.accept(handle);
                }
            }
        });
        return typedThis;
    }

    // Disambiguates the identically-signed define() inherited from Configurable (default) and
    // BindingsMixin (abstract). A per-statement define is per-render state, stored on the context's defines
    // overlay rather than the configuration, so it neither forks the copy-on-write config nor invalidates
    // memoized resolvers. Configuration-level defaults still come from Configurable#define (jdbi/handle level).
    @Override
    public This define(final String key, final Object value) {
        getContext().define(key, value);
        return typedThis;
    }

    // Disambiguates addCustomizer, which SqlStatement inherits from both Configurable (as a default)
    // and Customizable (as abstract). Keeps the Configurable behavior of recording on the (per-statement)
    // configuration copy.
    @Override
    public This addCustomizer(final StatementCustomizer customizer) {
        return configure(SqlStatements.class, c -> c.addCustomizer(customizer));
    }

    @Override
    @SafeVarargs
    public final <T> This bindArray(final int pos, final T... array) {
        return Customizable.super.bindArray(pos, array);
    }

    @Override
    @SafeVarargs
    public final <T> This bindArray(final String name, final T... array) {
        return Customizable.super.bindArray(name, array);
    }

    @Override
    public String toString() {
        return String.format("%s[sql=%s, bindings=%s]", getClass().getSimpleName(), sql, getContext().getBinding());
    }

    @SuppressWarnings("PMD.ExceptionAsFlowControl")
    PreparedStatement internalExecute() {
        final StatementContext ctx = getContext();
        final OptionalEvent evt = JfrSupport.newStatementEvent();
        evt.begin();

        beforeTemplating();

        final ParsedSql parsedSql = parseSql();

        final SqlStatements stmtConfig = getConfig(SqlStatements.class);
        try {
            prepareStatement(parsedSql);

            ctx.setStatement(stmt);

            beforeBinding();

            new ArgumentBinder(stmt, ctx, parsedSql.getParameters()).bind(getBinding());

            beforeExecution();

            attachJfrEvent(evt, ctx);

            try {
                SqlLoggerUtil.wrap(stmt::execute, ctx, stmtConfig.getSqlLogger());
            } catch (SQLException e) {
                throw stmtConfig.handleException(e, ctx);
            }

            afterExecution();

            return stmt;
        } catch (Exception e) {
            try {
                close();
            } catch (Exception e1) {
                e.addSuppressed(e1);
            }
            throw e;
        }
    }

    /**
     * Creates the JDBC statement, registers it for cleanup, and applies the configured statement customizers.
     * Shared by the single-execute engine ({@link #internalExecute()}) and the prepared-batch prologue. Sets
     * the {@link #stmt} field and returns it; the caller decides whether to record it on the context.
     */
    final PreparedStatement prepareStatement(final ParsedSql parsedSql) {
        final SqlStatements stmtConfig = getConfig(SqlStatements.class);
        try {
            stmt = createStatement(parsedSql.getSql());
            // The statement builder might (or might not) clean up the statement when called. E.g. the
            // caching statement builder relies on the statement *not* being closed.
            getContext().addCleanable(() -> cleanupStatement(stmt));
            stmtConfig.customize(stmt);
        } catch (SQLException e) {
            throw new UnableToCreateStatementException(e, getContext());
        }
        return stmt;
    }

    PreparedStatement createStatement(final String parsedSql) throws SQLException {
        return getHandle().getStatementBuilder().create(getHandle().getConnection(), parsedSql, getContext());
    }

    void cleanupStatement(final PreparedStatement statement) throws SQLException {
        getHandle().getStatementBuilder().close(getHandle().getConnection(), this.sql, statement);
    }

    ParsedSql parseSql() {
        final StatementContext ctx = getContext();

        if (cachedParsedSql != null && ctx.getDefinedAttributes().isEmpty()) {
            // Reusable-template fast path: the SQL was rendered and parsed once at build time, and this
            // execution supplied no per-execution defines (the only per-execution input to rendering), so
            // reuse them instead of re-rendering and re-parsing.
            ctx.setRenderedSql(cachedRenderedSql);
            ctx.setParsedSql(cachedParsedSql);
            return cachedParsedSql;
        }

        final SqlStatements statements = getConfig(SqlStatements.class);

        final String renderedSql = statements.preparedRender(sql, ctx.renderContext());
        ctx.setRenderedSql(renderedSql);

        final ParsedSql parsedSql = statements.getSqlParser().parse(renderedSql, ctx);
        ctx.setParsedSql(parsedSql);

        return parsedSql;
    }

    @SuppressWarnings("unchecked")
    <T> RowMapper<T> mapperForType(final Class<T> type) {
        return (RowMapper<T>) mapperForType((Type) type);
    }

    @SuppressWarnings("unchecked")
    <T> RowMapper<T> mapperForType(final GenericType<T> type) {
        return (RowMapper<T>) mapperForType(type.getType());
    }

    RowMapper<?> mapperForType(final Type type) {
        return getConfig().findMapperFor(type)
            .orElseThrow(() -> new UnsupportedOperationException("No mapper registered for " + type));
    }

    void beforeTemplating() {
        callCustomizers(c -> c.beforeTemplating(stmt, getContext()));
    }

    void beforeBinding() {
        callCustomizers(c -> c.beforeBinding(stmt, getContext()));
    }

    void beforeExecution() {
        callCustomizers(c -> c.beforeExecution(stmt, getContext()));
    }

    void afterExecution() {
        callCustomizers(c -> c.afterExecution(stmt, getContext()));
    }

    private void attachJfrEvent(final OptionalEvent statementEvent, final StatementContext ctx) {
        if (statementEvent.shouldCommit()) {
            new Object() {
                void attach() {
                    final var evt = (JdbiStatementEvent) statementEvent;
                    evt.traceId = ctx.getTraceId();
                    evt.type = ctx.describeJdbiStatementType();
                    final var stmtConfig = getConfig(SqlStatements.class);
                    final String renderedSql = ctx.getRenderedSql();
                    if (renderedSql != null) {
                        evt.sql = renderedSql.substring(0,
                                Math.min(renderedSql.length(), stmtConfig.getJfrSqlMaxLength()));
                    }
                    evt.parameters = getBinding().describe(stmtConfig.getJfrParamMaxLength());
                    ctx.addCleanable(() -> {
                        evt.rowsMapped = ctx.getMappedRows();
                        evt.commit();
                    });
                }
            }.attach();
        }
    }
}
