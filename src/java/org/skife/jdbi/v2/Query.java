/* Copyright 2004-2006 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.ResultSetException;
import org.skife.jdbi.v2.exceptions.UnableToCreateStatementException;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Query<ResultType>
{
    private final Connection connection;
    private final String sql;
    private final ResultSetMapper<ResultType> mapper;

    Query(ResultSetMapper<ResultType> mapper, Connection connection, String sql)
    {
        this.mapper = mapper;
        this.connection = connection;
        this.sql = sql;
    }

    /**
     *
     * @return
     * @throws UnableToCreateStatementException if there is an error creating the statement
     * @throws UnableToExecuteStatementException if there is an error executing the statement
     * @throws ResultSetException if there is an error dealing with the result set
     */
    public List<ResultType> list()
    {
        final PreparedStatement stmt;
        try
        {
            stmt = connection.prepareStatement(sql);
        }
        catch (SQLException e)
        {
            throw new UnableToCreateStatementException(e);
        }
        ResultSet rs;
        try
        {
            rs = stmt.executeQuery();
        }
        catch (SQLException e)
        {
            throw new UnableToExecuteStatementException(e);
        }

        try
        {
            List<ResultType> result_list = new ArrayList<ResultType>();
            int index = 0;
            while (rs.next()){
                result_list.add(mapper.map(index++, rs));
            }
            return result_list;
        }
        catch (SQLException e)
        {
            throw new ResultSetException("Exception thrown while attempting to traverse the result set", e);
        }
    }

    public <T> Query<T> map(Class<T> resultType)
    {
        return new Query<T>(new BeanMapper<T>(resultType), connection, sql);
    }
}
