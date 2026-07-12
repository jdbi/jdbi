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
import java.util.HashMap;
import java.util.Map;

import org.jdbi.core.Handle;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.result.ResultIterable;
import org.jdbi.core.result.ResultSetScanner;
import org.jdbi.core.result.UnableToProduceResultException;

/**
 * A single, thread-confined execution of a {@link QueryTemplate} against a specific {@link Handle}.
 * Obtained from {@link QueryTemplate#with(Handle)}. Bind parameters (and optionally override defined
 * attributes with {@link #define}), then call {@link #execute()}. The template's configuration and
 * pre-parsed SQL are shared read-only; only per-execution state (bindings, defines, the statement
 * context, the JDBC statement) lives here.
 */
public class QueryTemplateBinding<R> implements BindingsMixin<QueryTemplateBinding<R>> {
    private final Handle handle;
    private final StatementContext ctx;
    private final Binding binding;
    private final String sql;
    private final ConfigRegistry config;
    private final String renderedSql;
    private final ParsedSql parsedSql;
    private final ResultSetScanner<ResultIterable<R>> scanner;
    private final Map<String, Object> defines = new HashMap<>();

    QueryTemplateBinding(final Handle handle, final QueryTemplate<R> template) {
        this.handle = handle;
        this.sql = template.builder.getSql();
        this.config = template.builder.getConfig();
        this.renderedSql = template.renderedSql;
        this.parsedSql = template.parsedSql;
        this.scanner = template.scanner;
        this.ctx = StatementContext.create(config, handle.getExtensionMethod(), getClass())
            .setConnection(handle.getConnection())
            .setRawSql(sql);
        this.binding = new Binding(ctx);
    }

    @Override
    public Binding getBinding() {
        return binding;
    }

    @Override
    public ConfigRegistry getConfig() {
        return config;
    }

    /**
     * Overrides a defined attribute for this execution only. The template's configuration is not
     * affected. Defining any attribute causes the SQL to be re-rendered for this execution instead
     * of reusing the template's pre-rendered SQL.
     *
     * @param key   the attribute name
     * @param value the attribute value
     * @return this
     */
    @Override
    public QueryTemplateBinding<R> define(final String key, final Object value) {
        defines.put(key, value);
        return this;
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

        // Constant-defines fast path: reuse the SQL rendered and parsed once at template build time.
        // If this execution overrode any define, re-render and re-parse with the overlay applied.
        final ParsedSql effectiveParsedSql;
        if (defines.isEmpty()) {
            ctx.setRenderedSql(renderedSql);
            effectiveParsedSql = parsedSql;
        } else {
            final String rendered = stmtConfig.preparedRender(sql, new RenderContext(config, defines));
            ctx.setRenderedSql(rendered);
            effectiveParsedSql = stmtConfig.getSqlParser().parse(rendered, ctx);
        }
        ctx.setParsedSql(effectiveParsedSql);

        try {
            final PreparedStatement stmt = handle.getStatementBuilder()
                .create(handle.getConnection(), effectiveParsedSql.getSql(), ctx);
            ctx.addCleanable(() -> handle.getStatementBuilder().close(handle.getConnection(), sql, stmt));
            stmtConfig.customize(stmt);
            ctx.setStatement(stmt);

            new ArgumentBinder(stmt, ctx, effectiveParsedSql.getParameters()).bind(binding);

            SqlLoggerUtil.wrap(stmt::execute, ctx, stmtConfig.getSqlLogger());

            return stmt.getResultSet();
        } catch (final SQLException e) {
            throw stmtConfig.handleException(e, ctx);
        }
    }
}
