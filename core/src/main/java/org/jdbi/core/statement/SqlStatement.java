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
import org.jdbi.core.generic.GenericType;
import org.jdbi.core.mapper.Mappers;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.statement.internal.JfrSupport;
import org.jdbi.core.statement.internal.OptionalEvent;

/**
 * This class provides the common functions between <code>Query</code> and
 * <code>Update</code>. It defines most of the argument binding functions
 * used by its subclasses.
 */
@SuppressWarnings({"PMD.ConfusingArgumentToVarargsMethod"})
public abstract class SqlStatement<This extends SqlStatement<This>> extends BaseStatement<This> implements BindingsMixin<This> {
    private final String sql;
    PreparedStatement stmt;

    SqlStatement(final Handle handle,
                 final CharSequence sql) {
        super(handle);

        this.sql = sql.toString();

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

    @Override
    @SafeVarargs
    public final <T> This bindArray(final int pos, final T... array) {
        return BindingsMixin.super.bindArray(pos, array);
    }

    @Override
    @SafeVarargs
    public final <T> This bindArray(final String name, final T... array) {
        return BindingsMixin.super.bindArray(name, array);
    }

    @Override
    public String toString() {
        return String.format("%s[sql=%s, bindings=%s]", getClass().getSimpleName(), sql, getContext().getBinding());
    }

    PreparedStatement internalExecute() {
        final StatementContext ctx = getContext();
        final OptionalEvent evt = JfrSupport.newStatementEvent();
        evt.begin();

        beforeTemplating();

        final ParsedSql parsedSql = parseSql();

        try {
            stmt = createStatement(parsedSql.getSql());
            // The statement builder might (or might not) clean up the statement when called. E.g. the
            // caching statement builder relies on the statement *not* being closed.
            getContext().addCleanable(() -> cleanupStatement(stmt));
            getConfig(SqlStatements.class).customize(stmt);
        } catch (final SQLException e) {
            throw new UnableToCreateStatementException(e, ctx);
        }

        ctx.setStatement(stmt);

        beforeBinding();

        new ArgumentBinder(stmt, ctx, parsedSql.getParameters()).bind(getBinding());

        beforeExecution();

        attachJfrEvent(evt, ctx);

        try {
            SqlLoggerUtil.wrap(stmt::execute, ctx, getConfig(SqlStatements.class).getSqlLogger());
        } catch (final SQLException e) {
            throw new UnableToExecuteStatementException(e, ctx);
        }

        afterExecution();

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
        final SqlStatements statements = getConfig(SqlStatements.class);

        final String renderedSql = statements.preparedRender(sql, ctx.getConfig());
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
        return getConfig(Mappers.class).findFor(type)
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
