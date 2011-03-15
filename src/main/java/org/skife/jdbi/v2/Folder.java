/*
 * Copyright 2004 - 2011 Brian McCallister
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

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Used to define a function for <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">folding</a>
 * over the result set of a query.
 * @see org.skife.jdbi.v2.Query#fold(Object, Folder)
 * @deprecated Prefer Folder2, in jdbi3 Folder2 will be renamed Folder and this form will go away
 */
@Deprecated
public interface Folder<AccumulatorType>
{
    /**
     * Invoked once per row in the result set from the query.
     *
     * @param accumulator The initial value passed to {@link org.skife.jdbi.v2.Query#fold(Object, Folder)}
     *                    for the first call, the return value from the previous call thereafter.
     * @param rs The actual result set from the query. It will already have been advanced to the
     *           correct row. Callbacks should not call {@link java.sql.ResultSet#next()}
     * @return A value which will be passed to the next invocation of this function. The final
     *         invocation will be returned from the {@link org.skife.jdbi.v2.Query#fold(Object, Folder)} call.
     * @throws SQLException will be wrapped and rethrown as a {@link org.skife.jdbi.v2.exceptions.CallbackFailedException}
     */
    AccumulatorType fold(AccumulatorType accumulator, ResultSet rs) throws SQLException;
}
