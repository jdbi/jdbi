package org.skife.jdbi.v2;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 */
public interface Folder2<AccumulatorType>
{
    /**
     * Invoked once per row in the result set from the query.
     *
     * @param accumulator The initial value passed to {@link org.skife.jdbi.v2.Query#fold(Object, Folder)}
     *                    for the first call, the return value from the previous call thereafter.
     * @param rs The actual result set from the query. It will already have been advanced to the
     *           correct row. Callbacks should not call {@link java.sql.ResultSet#next()}
     * @param ctx The statement context for execution
     * @return A value which will be passed to the next invocation of this function. The final
     *         invocation will be returned from the {@link org.skife.jdbi.v2.Query#fold(Object, Folder)} call.
     * @throws java.sql.SQLException will be wrapped and rethrown as a
               {@link org.skife.jdbi.v2.exceptions.CallbackFailedException
     */
    AccumulatorType fold(AccumulatorType accumulator, ResultSet rs, StatementContext ctx) throws SQLException;

}
