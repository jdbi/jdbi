package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.util.Map;

/**
 * Convenience class which allows definition of result set mappers which get the
 * row as a map instead of a result set. This can be useful.
 */
public abstract class BaseResultSetMapper<ResultType> implements ResultSetMapper<ResultType>
{
    private static final DefaultMapper mapper = new DefaultMapper();

    /**
     * Defers to {@link BaseResultSetMapper#mapInternal(int, java.util.Map<java.lang.String,java.lang.Object>)}
     */
    public final ResultType map(int index, ResultSet r)
    {
        return this.mapInternal(index, mapper.map(index, r));
    }

    /**
     * Subclasses should implement this method in order to map the result
     *
     * @param index The row, starting at 0
     * @param row The result of a {@link DefaultMapper#map} call
     * @return the value to pt into the results from a query
     */
    protected abstract ResultType mapInternal(int index, Map<String, Object> row);
}
