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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdbi.core.Handle;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.result.ResultBearing;
import org.jdbi.core.result.ResultSetScanner;
import org.jdbi.core.result.UnableToProduceResultException;
import org.jdbi.core.statement.BaseStatement.StatementCustomizerInvocation;

/**
 * A single, thread-confined execution of a {@link QueryTemplate} against a specific {@link Handle}.
 * Obtained from {@link QueryTemplate#with(Handle)}. Bind parameters (and optionally override defined
 * attributes with {@link #define} or set query customizers such as {@link #setFetchSize}), then apply
 * a result operation inherited from {@link ResultBearing} &mdash; for example {@link #mapTo(Class)},
 * {@link #reduceRows}, or {@link #collectInto(Class)}.
 *
 * <p>The template's configuration and pre-parsed SQL are shared read-only; only per-execution state
 * (bindings, defines, statement customizers, the statement context, the JDBC statement) lives here.
 * In particular, customizers added here are recorded on the binding rather than on the shared
 * configuration, so they affect only this execution.
 */
public class QueryTemplateBinding implements ResultBearing, BindingsMixin<QueryTemplateBinding> {
    private final Handle handle;
    private final QueryTemplate template;
    private final StatementContext ctx;
    private final Binding binding;
    private final Map<String, Object> defines = new HashMap<>();
    private final List<StatementCustomizer> customizers = new ArrayList<>();

    QueryTemplateBinding(final Handle handle, final QueryTemplate template) {
        this.handle = handle;
        this.template = template;
        this.ctx = StatementContext.create(template.config, handle.getExtensionMethod(), getClass())
            .setConnection(handle.getConnection())
            .setRawSql(template.sql);
        this.binding = new Binding(ctx);
    }

    @Override
    public Binding getBinding() {
        return binding;
    }

    @Override
    public ConfigRegistry getConfig() {
        return template.config;
    }

    /**
     * @return the statement context for this execution.
     */
    public StatementContext getContext() {
        return ctx;
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
    public QueryTemplateBinding define(final String key, final Object value) {
        defines.put(key, value);
        return this;
    }

    /**
     * Adds a statement customizer for this execution only. Unlike {@link org.jdbi.core.config.Configurable#addCustomizer},
     * the customizer is recorded on the binding rather than on the shared configuration, so it does not
     * affect other executions of the same template.
     *
     * @param customizer the customizer to apply to this execution's JDBC statement
     * @return this
     */
    public QueryTemplateBinding addCustomizer(final StatementCustomizer customizer) {
        customizers.add(customizer);
        return this;
    }

    /**
     * Sets the fetch size for this execution. See {@link java.sql.Statement#setFetchSize(int)}.
     *
     * @param fetchSize the number of rows to fetch per round trip
     * @return this
     */
    public QueryTemplateBinding setFetchSize(final int fetchSize) {
        return addCustomizer(StatementCustomizers.fetchSize(fetchSize));
    }

    /**
     * Sets the maximum number of rows for this execution. See {@link java.sql.Statement#setMaxRows(int)}.
     *
     * @param maxRows the maximum number of rows to return
     * @return this
     */
    public QueryTemplateBinding setMaxRows(final int maxRows) {
        return addCustomizer(StatementCustomizers.maxRows(maxRows));
    }

    /**
     * Sets the maximum field size for this execution. See {@link java.sql.Statement#setMaxFieldSize(int)}.
     *
     * @param maxFieldSize the maximum field size in bytes
     * @return this
     */
    public QueryTemplateBinding setMaxFieldSize(final int maxFieldSize) {
        return addCustomizer(StatementCustomizers.maxFieldSize(maxFieldSize));
    }

    /**
     * Sets the query timeout for this execution. See {@link java.sql.Statement#setQueryTimeout(int)}.
     *
     * @param seconds the timeout in seconds
     * @return this
     */
    public QueryTemplateBinding setQueryTimeout(final int seconds) {
        return addCustomizer(StatementCustomizers.statementTimeout(seconds));
    }

    @Override
    @SafeVarargs
    public final <T> QueryTemplateBinding bindArray(final int pos, final T... array) {
        return BindingsMixin.super.bindArray(pos, array);
    }

    @Override
    @SafeVarargs
    public final <T> QueryTemplateBinding bindArray(final String name, final T... array) {
        return BindingsMixin.super.bindArray(name, array);
    }

    /**
     * Runs the query and hands its result set to the given scanner. This is the single primitive on
     * which every {@link ResultBearing} operation ({@code mapTo}, {@code reduceRows}, {@code collectInto},
     * &hellip;) is built. The statement is not created or executed until the scanner pulls the result set;
     * pulling it also closes the statement context and releases the underlying JDBC resources.
     *
     * @param resultSetScanner the scanner consuming the result set
     * @param <R>              the scanner's result type
     * @return the value produced by the scanner
     */
    @Override
    public <R> R scanResultSet(final ResultSetScanner<R> resultSetScanner) {
        try {
            return resultSetScanner.scanResultSet(this::executeStatement, ctx);
        } catch (final SQLException e) {
            throw new UnableToProduceResultException(e, ctx);
        }
    }

    private ResultSet executeStatement() {
        final SqlStatements stmtConfig = template.config.get(SqlStatements.class);

        callCustomizers(stmtConfig, c -> c.beforeTemplating(null, ctx));

        // Constant-defines fast path: reuse the SQL rendered and parsed once at template build time.
        // If this execution overrode any define, re-render and re-parse with the overlay applied.
        final ParsedSql effectiveParsedSql;
        if (defines.isEmpty()) {
            ctx.setRenderedSql(template.renderedSql);
            effectiveParsedSql = template.parsedSql;
        } else {
            final String rendered = stmtConfig.preparedRender(template.sql, new RenderContext(template.config, defines));
            ctx.setRenderedSql(rendered);
            effectiveParsedSql = stmtConfig.getSqlParser().parse(rendered, ctx);
        }
        ctx.setParsedSql(effectiveParsedSql);

        try {
            final PreparedStatement stmt = handle.getStatementBuilder()
                .create(handle.getConnection(), effectiveParsedSql.getSql(), ctx);
            ctx.addCleanable(() -> handle.getStatementBuilder().close(handle.getConnection(), template.sql, stmt));
            stmtConfig.customize(stmt);
            ctx.setStatement(stmt);

            callCustomizers(stmtConfig, c -> c.beforeBinding(stmt, ctx));

            new ArgumentBinder(stmt, ctx, effectiveParsedSql.getParameters()).bind(binding);

            callCustomizers(stmtConfig, c -> c.beforeExecution(stmt, ctx));

            SqlLoggerUtil.wrap(stmt::execute, ctx, stmtConfig.getSqlLogger());

            callCustomizers(stmtConfig, c -> c.afterExecution(stmt, ctx));

            return stmt.getResultSet();
        } catch (final SQLException e) {
            throw stmtConfig.handleException(e, ctx);
        }
    }

    /**
     * Invokes the given lifecycle hook on every applicable customizer: first those registered on the
     * shared configuration, then those added to this binding for this execution only.
     */
    private void callCustomizers(final SqlStatements stmtConfig, final StatementCustomizerInvocation invocation) {
        for (final StatementCustomizer customizer : stmtConfig.getCustomizers()) {
            invoke(customizer, invocation);
        }
        for (final StatementCustomizer customizer : customizers) {
            invoke(customizer, invocation);
        }
    }

    private void invoke(final StatementCustomizer customizer, final StatementCustomizerInvocation invocation) {
        try {
            invocation.call(customizer);
        } catch (final SQLException e) {
            throw new UnableToExecuteStatementException("Exception thrown in statement customization", e, ctx);
        }
    }
}
