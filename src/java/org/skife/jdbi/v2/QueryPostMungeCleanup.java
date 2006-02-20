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

import org.skife.jdbi.v2.exceptions.UnableToCloseResourceException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

interface QueryPostMungeCleanup
{
    final QueryPostMungeCleanup CLOSE_RESOURCES = new QueryPostMungeCleanup()
    {
        public void cleanup(Query query, PreparedStatement stmt, ResultSet rs)
        {
            SQLException from_closing_result_set = null;
            if (rs != null)
            {
                try
                {
                    rs.close();
                }
                catch (SQLException e)
                {
                    from_closing_result_set = e;
                }
            }

            SQLException from_closing_statement = null;
            if (stmt != null)
            {
                try
                {
                    stmt.close();
                }
                catch (SQLException e)
                {
                    from_closing_statement = e;
                }
            }
            if (from_closing_statement == null && from_closing_result_set == null)
            {
                // went smoothly
            }
            else if (from_closing_result_set != null && from_closing_statement == null)
            {
                // closing rs errored
                throw new UnableToCloseResourceException("Unable to close result set",
                                                         from_closing_result_set);
            }
            else if (from_closing_result_set == null)
            {
                // closing stmt errored
                throw new UnableToCloseResourceException("Unable to close statement",
                                                         from_closing_statement);
            }
            else
            {
                // both errored!
                throw new UnableToCloseResourceException("Exceptions closing both statement and result set, " +
                                                         "get cause for result set exception, getOtherException() " +
                                                         "for the statement exception",
                                                         from_closing_result_set,
                                                         from_closing_statement);
            }
        }
    };

    void cleanup(Query query, PreparedStatement stmt, ResultSet rs);
}
