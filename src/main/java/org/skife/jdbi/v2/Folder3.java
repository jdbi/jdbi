/*
 * Copyright (C) 2004 - 2014 Brian McCallister
 *
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
