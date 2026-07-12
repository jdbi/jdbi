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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.core.Handle;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.result.ResultIterable;
import org.jdbi.core.result.ResultSetScanner;
import org.jdbi.core.result.UnableToProduceResultException;

/**
 * A single, thread-confined execution of a {@link QueryTemplate} against a specific {@link Handle}.
 * Obtained from {@link QueryTemplate#with(Handle)}. Bind parameters, then call {@link #execute()}.
 * The template's configuration is shared read-only; only per-execution state (bindings, the
 * statement context, the JDBC statement) lives here.
 */
public class QueryTemplateBinding<R> implements BindingsMixin<QueryTemplateBinding<R>> {
    private final Handle handle;
    private final StatementContext ctx;
    private final Binding binding;
    private final String sql;
    private final ConfigRegistry config;
    private final ResultSetScanner<ResultIterable<R>> scanner;

    QueryTemplateBinding(final Handle handle, final QueryTemplate<R> template) {
        this.handle = handle;
        sql = template.builder.getSql();
        config = template.builder.getConfig();
        scanner = template.scanner;
        ctx = StatementContext.create(config, handle.getExtensionMethod(), getClass())
            .setConnection(handle.getConnection())
            .setRawSql(this.sql);
        binding = new Binding(ctx);
    }

    @Override
    public Binding getBinding() {
        return binding;
    }

    @Override
    public ConfigRegistry getConfig() {
        return config;
    }

    @Override
    @SafeVarargs
    public final <T> QueryTemplateBinding<R> bindArray(final int pos, final T... array) {
        return BindingsMixin.super.bindArray(pos, array);
    }

    @Override
    @SafeVarargs
    public final <T> QueryTemplateBinding<R> bindArray(final String name, final T... array) {
        return BindingsMixin.super.bindArray(name, array);
    }

    /**
     * Execute the query, returning a lazily-evaluated {@link ResultIterable} over the result rows.
     * The statement is not created or executed until the result is consumed; consuming it also
     * closes the statement context and releases the underlying JDBC resources.
     *
     * @return the query results.
     */
    public ResultIterable<R> execute() {
        try {
            return scanner.scanResultSet(this::executeStatement, ctx);
        } catch (final SQLException e) {
            throw new UnableToProduceResultException(e, ctx);
        }
    }

    private ResultSet executeStatement() {
        final SqlStatements stmtConfig = config.get(SqlStatements.class);

        final String renderedSql = stmtConfig.preparedRender(sql, config);
        ctx.setRenderedSql(renderedSql);

        final ParsedSql parsedSql = stmtConfig.getSqlParser().parse(renderedSql, ctx);
        ctx.setParsedSql(parsedSql);

        try {
            final PreparedStatement stmt = handle.getStatementBuilder()
                .create(handle.getConnection(), parsedSql.getSql(), ctx);
            ctx.addCleanable(() -> handle.getStatementBuilder().close(handle.getConnection(), sql, stmt));
            stmtConfig.customize(stmt);
            ctx.setStatement(stmt);

            new ArgumentBinder(stmt, ctx, parsedSql.getParameters()).bind(binding);

            SqlLoggerUtil.wrap(stmt::execute, ctx, stmtConfig.getSqlLogger());

            return stmt.getResultSet();
        } catch (final SQLException e) {
            throw stmtConfig.handleException(e, ctx);
        }
    }
}
