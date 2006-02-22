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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

class DynamicStatementEnvelope implements StatementEnvelope
{
    private Set statements = new HashSet();
    private final Connection conn;
    private final StatementParser parser;

    DynamicStatementEnvelope(Connection conn, StatementParser parser)
    {
        this.conn = conn;

        this.parser = parser;
    }

    public PreparedStatement prepare(final Arguments args) throws SQLException
    {
        String sql = parser.getSubstitutedSql();

        Object[] params = args.objects();

        PARAM_LOOP:
        for (int i = 0; i < params.length; i++)
        {
            final Object param = params[i];
            if (param instanceof Collection)
            {
                final Collection in_param = (Collection) param;
                final Object[] to_insert = new Object[in_param.size()];
                int index = 0;
                for (Iterator iterator = in_param.iterator(); iterator.hasNext();)
                {
                    final Object o = iterator.next();
                    to_insert[index++] = o;
                }
                final Object[] new_params = new Object[params.length - 1 + to_insert.length];
                System.arraycopy(params, 0, new_params, 0, i + 1);
                System.arraycopy(to_insert, 0, new_params, i, to_insert.length);
                params = new_params;
                int index_of_sub = StatementFactory.findNth(sql, "?", i + 1);
                String first = sql.substring(0, index_of_sub);
                StringBuffer buffer = new StringBuffer();
                for (int j = 1; j < new_params.length; j++)
                {
                    buffer.append("?");
                    if (j < new_params.length)
                    {
                        buffer.append(",");
                    }
                }
                String subs = buffer.toString();
                String last = sql.substring(index_of_sub, sql.length());
                sql = first + subs + last;

                break PARAM_LOOP;
            }
            else if (param.getClass().isArray())
            {
                final Collection in_param = Arrays.asList((Object[]) param);
                final Object[] to_insert = new Object[in_param.size()];
                int index = 0;
                for (Iterator iterator = in_param.iterator(); iterator.hasNext();)
                {
                    final Object o = iterator.next();
                    to_insert[index++] = o;
                }
                final Object[] new_params = new Object[params.length - 1 + to_insert.length];
                System.arraycopy(params, 0, new_params, 0, i + 1);
                System.arraycopy(to_insert, 0, new_params, i, to_insert.length);
                params = new_params;
                int index_of_sub = StatementFactory.findNth(sql, "?", i + 1);
                String first = sql.substring(0, index_of_sub);
                StringBuffer buffer = new StringBuffer();
                for (int j = 1; j < new_params.length; j++)
                {
                    buffer.append("?");
                    if (j < new_params.length)
                    {
                        buffer.append(",");
                    }
                }
                String subs = buffer.toString();
                String last = sql.substring(index_of_sub, sql.length());
                sql = first + subs + last;

                break PARAM_LOOP;
            }
        }

        System.err.println("DSQL: " + sql);
        PreparedStatement stmt = sql.toUpperCase().trim().startsWith("CALL")
                                 ? conn.prepareCall(sql)
                                 : conn.prepareStatement(sql);

        for (int i = 0; i < params.length; i++)
        {
            final Object param = params[i];
            if (param != null)
            {
                stmt.setObject(i + 1, param);
            }
            else
            {
                stmt.setNull(i + 1, Types.OTHER);
            }
        }
        statements.add(stmt);
        return stmt;
    }

    public void close() throws SQLException
    {
        SQLException e = null;
        for (Iterator iterator = statements.iterator(); iterator.hasNext();)
        {
            final PreparedStatement preparedStatement = (PreparedStatement) iterator.next();
            try
            {
                preparedStatement.close();
            }
            catch (SQLException e1)
            {
                e = e1;
            }
        }
        if (e != null) throw e;
    }

    public String[] getNamedParameters()
    {
        return parser.getNamedParams();
    }

}
