package org.jdbi.v3.core.statement;

import java.sql.SQLException;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.result.ResultSetScanner;
import org.jdbi.v3.core.result.UnableToProduceResultException;

public class QueryTemplateBinding<R> implements BindingsMixin<QueryTemplateBinding<R>> {
    private final StatementContext ctx;
    private final Binding binding;
    private final String sql;
    private final ConfigRegistry config;
    private final ResultSetScanner<ResultIterable<R>> scanner;

    QueryTemplateBinding(final Handle handle, final QueryTemplate<R> template) {
        sql = template.builder.getSql();
        config = template.builder.getConfig();
        scanner = template.scanner;
        ctx = StatementContext.create(config, handle.getExtensionMethod())
            .setConnection(handle.getConnection())
            .setRawSql(this.sql);
        binding = new Binding(ctx);
    }

    @Override
    public Binding getBinding() {
        return binding;
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

    public ResultIterable<R> execute() {
        new Query(null, sql)
        try {
            return scanner.scanResultSet(null, ctx);
        } catch (final SQLException e) {
            throw new UnableToProduceResultException(e, ctx);
        }
    }
}
