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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

class StaticStatementEnvelope implements StatementEnvelope
{
    final PreparedStatement stmt;
    final StatementParser parser;

    StaticStatementEnvelope(Connection conn, StatementParser parser) throws SQLException
    {
        this.parser = parser;
        final String real_sql = parser.getSubstitutedSql();
        stmt = real_sql.toUpperCase().trim().startsWith("CALL")
               ? conn.prepareCall(real_sql)
               : conn.prepareStatement(real_sql);
    }

    public PreparedStatement prepare(final Arguments args) throws SQLException
    {
        final Object[] params = args.objects();
        stmt.clearParameters();
        for (int i = 0; i < params.length; i++)
        {
            final Object param = params[i];
            if (param == null)
            {
                stmt.setNull(i + 1, Types.OTHER);
            }
            else
            {
                stmt.setObject(i + 1, param);
            }
        }
        return stmt;
    }

    public void close() throws SQLException
    {
        stmt.close();
    }

    public String[] getNamedParameters()
    {
        return parser.getNamedParams();
    }
}
