/* Copyright 2004-2005 Brian McCallister
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
package org.skife.jdbi;

import java.sql.Connection;
import java.sql.SQLException;

class StatementFactory
{
    static StatementEnvelope build(final Connection conn, final String sql) throws SQLException
    {
        final StatementParser parser = new StatementParser(sql);
        return new StaticStatementEnvelope(conn, parser);
//        if (parser.isDynamic())
//        {
//            return new DynamicStatementEnvelope(conn, parser);
//        }
//        else
//        {
//            return new StaticStatementEnvelope(conn, parser);
//        }
    }

    static int findNth(String sql, String s, int n)
    {
        int loc = 0;
        for (int i = 0; i != n; ++i)
        {
            loc = sql.indexOf(s, 0);
        }
        return loc;
    }
}
