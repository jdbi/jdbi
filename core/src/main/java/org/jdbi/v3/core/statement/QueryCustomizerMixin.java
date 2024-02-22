package org.jdbi.v3.core.statement;

import java.sql.Statement;

import org.jdbi.v3.core.config.Configurable;

public interface QueryCustomizerMixin<This> extends Configurable<This> {

    /**
     * Specify the fetch size for the query. This should cause the results to be
     * fetched from the underlying RDBMS in groups of rows equal to the number passed.
     * This is useful for doing chunked streaming of results when exhausting memory
     * could be a problem.
     *
     * @param fetchSize the number of rows to fetch in a bunch
     *
     * @return the modified query
     */
    default This setFetchSize(final int fetchSize) {
        return addCustomizer(StatementCustomizers.fetchSize(fetchSize));
    }

    /**
     * Specify the maximum number of rows the query is to return. This uses the underlying JDBC
     * {@link Statement#setMaxRows(int)}}.
     *
     * @param maxRows maximum number of rows to return
     *
     * @return modified query
     */
    default This setMaxRows(final int maxRows) {
        return addCustomizer(StatementCustomizers.maxRows(maxRows));
    }

    /**
     * Specify the maximum field size in the result set. This uses the underlying JDBC
     * {@link Statement#setMaxFieldSize(int)}
     *
     * @param maxFields maximum field size
     *
     * @return modified query
     */
    default This setMaxFieldSize(final int maxFields) {
        return addCustomizer(StatementCustomizers.maxFieldSize(maxFields));
    }

}
