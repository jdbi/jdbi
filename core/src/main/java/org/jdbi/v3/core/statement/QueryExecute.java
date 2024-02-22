package org.jdbi.v3.core.statement;

import org.jdbi.v3.core.result.ResultProducer;

public interface QueryExecute {

    /**
     * Executes the query, returning the result obtained from the given {@link ResultProducer}.
     *
     * @param <R> the type of the result
     * @param producer the result producer.
     * @return value returned by the result producer.
     */
    <R> R execute(ResultProducer<R> producer);

    /**
     * Specify that the result set should be concurrent updatable.
     *
     * This will allow the update methods to be called on the result set produced by this
     * Query.
     *
     * @return the modified query
     */
    Query concurrentUpdatable();
}
