package org.skife.jdbi.v2;

import java.sql.SQLException;

public interface Folder3<AccumulatorType, MappedType>
{
    /**
     * Invoked once per row in the result set from the query.
     *
     * @param accumulator The initial value passed to {@link org.skife.jdbi.v2.Query#fold(Object, Folder)}
     *                    for the first call, the return value from the previous call thereafter.
     * @param rs The mapped result set row to fold across
     * @param ctx The statement context for execution
     * @return A value which will be passed to the next invocation of this function. The final
     *         invocation will be returned from the {@link org.skife.jdbi.v2.Query#fold(Object, Folder)} call.
     * @throws java.sql.SQLException will be wrapped and rethrown as a {@link org.skife.jdbi.v2.exceptions.CallbackFailedException}
     */
    AccumulatorType fold(AccumulatorType accumulator,
                         MappedType rs,
                         FoldController control,
                         StatementContext ctx) throws SQLException;
}
