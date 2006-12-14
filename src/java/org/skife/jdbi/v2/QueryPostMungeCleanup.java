/*
 * Copyright 2004-2006 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.sql.Statement;

interface QueryPostMungeCleanup
{
    final QueryPostMungeCleanup CLOSE_RESOURCES_QUIETLY = new QueryPostMungeCleanup()
    {
        public void cleanup(SQLStatement query, Statement stmt, ResultSet rs)
        {
            if (rs != null)
            {
                try
                {
                    rs.close();
                }
                catch (SQLException e)
                {
                    // nothing we can do
                }
            }

            if (stmt != null)
            {
                try
                {
                    stmt.close();
                }
                catch (SQLException e)
                {
                    // nothing we can do
                }
            }
        }
    };
    final QueryPostMungeCleanup NO_OP =  new QueryPostMungeCleanup()
    {
        public void cleanup(SQLStatement query, Statement stmt, ResultSet rs)
        {

        }
    };

    void cleanup(SQLStatement query, Statement stmt, ResultSet rs);
}
