package org.jdbi.v3.core.statement;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.result.ResultSetScanner;

public class QueryTemplate<R> {
    final QueryTemplateBuilder builder;
    final ResultSetScanner<ResultIterable<R>> scanner;

    QueryTemplate(final QueryTemplateBuilder builder, final ResultSetScanner<ResultIterable<R>> scanner) {
        this.builder = builder;
        this.scanner = scanner;
    }

    public QueryTemplateBinding<R> with(final Handle handle) {
        return new QueryTemplateBinding<>(handle, this);
    }
}
