/*
 * Copyright (C) 2004 - 2013 Brian McCallister
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
package org.skife.jdbi.v3;

import org.skife.jdbi.v3.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.util.Map;

/**
 * Convenience class which allows definition of result set mappers which getAttribute the
 * row as a map instead of a result set. This can be useful.
 */
public abstract class BaseResultSetMapper<ResultType> implements ResultSetMapper<ResultType>
{
    private static final DefaultMapper mapper = new DefaultMapper();

    /**
     * Defers to mapInternal
     */
    public final ResultType map(int index, ResultSet r, StatementContext ctx)
    {
        return this.mapInternal(index, mapper.map(index, r, ctx));
    }

    /**
     * Subclasses should implement this method in order to map the result
     *
     * @param index The row, starting at 0
     * @param row The result of a {@link org.skife.jdbi.v3.tweak.ResultSetMapper#map} call
     * @return the value to pt into the results from a query
     */
    protected abstract ResultType mapInternal(int index, Map<String, Object> row);
}
