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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class StatementCache
{
    private final Connection conn;
    private final Map envelopes;
    private final NamedStatementRepository repository;

    StatementCache(final Connection conn)
    {
        this(conn, new NamedStatementRepository());
    }

    StatementCache(final Connection conn, final NamedStatementRepository repository)
    {
        this.conn = conn;
        envelopes = new HashMap();
        this.repository = repository;
    }


    Collection close()
    {
        final ArrayList exceptions = new ArrayList();
        for (Iterator iterator = envelopes.entrySet().iterator(); iterator.hasNext();)
        {
            final Map.Entry entry = (Map.Entry) iterator.next();
            final StatementEnvelope stmt = (StatementEnvelope) entry.getValue();
            try
            {
                stmt.close();
            }
            catch (SQLException e)
            {
                exceptions.add(e);
            }
        }
        envelopes.clear();
        return exceptions;
    }

    private StatementEnvelope locateStatement(final String statement) throws DBIException
    {
        if (this.envelopes.containsKey(statement))
        {
            return (StatementEnvelope) this.envelopes.get(statement);
        }
        if (repository.contains(statement))
        {
            final String sql = repository.get(statement);
            try
            {
                final StatementEnvelope stmt = this.store(statement, sql);
                return stmt;
            }
            catch (SQLException e1)
            {
                throw new DBIException("unable to parse statement stored in global repository: " +
                                       e1.getMessage(), e1);
            }
        }
        final String lower_case = statement.toLowerCase().trim();
        if (lower_case.startsWith("select ")
            || lower_case.startsWith("update ")
            || lower_case.startsWith("insert ")
            || lower_case.startsWith("delete ")
            || lower_case.startsWith("call "))
        {
            try
            {
                return  this.store(statement, statement);
            }
            catch (SQLException e)
            {
                // may be a very weird named statement
            }
        }
        try
        {
            final String sql = load(statement);
            final StatementEnvelope stmt = this.store(statement, sql);
            repository.store(statement, sql);
            return stmt;
        }
        catch (Exception er)
        {
            throw new DBIException("unable to parse, or findInternal, statement [" + statement + "]", er);
        }

    }

    /**
     * @return the sql string loaded from the file
     */
    String load(final String name) throws IOException
    {
        final ClassLoader loader;
        loader = selectClassLoader();
        InputStream in_stream;
        in_stream = loader.getResourceAsStream(name + ".sql");
        if (in_stream == null)
        {
            in_stream = loader.getResourceAsStream(name);
        }
        if (in_stream == null)
        {
            throw new IllegalArgumentException("unable to locate external sql [" + name + ".sql]");
        }
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in_stream));
        final StringBuffer buffer = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null)
        {
            if (isComment(line))
            {
                // comment
                continue;
            }
            buffer.append(line).append(" ");
        }
        reader.close();
        final String sql = buffer.toString();
        return sql;
    }

    static boolean isComment(final String line)
    {
        return line.startsWith("#") || line.startsWith("--") || line.startsWith("//");
    }

    /**
     * There *must* be a better place to put this without creating a util class just for it
     */
    static ClassLoader selectClassLoader()
    {
        ClassLoader loader;
        if (Thread.currentThread().getContextClassLoader() != null)
        {
            loader = Thread.currentThread().getContextClassLoader();
        }
        else
        {
            loader = StatementCache.class.getClassLoader();
        }
        return loader;
    }

    void name(final String name, final String sql) throws DBIException
    {
        try
        {
            this.store(name, sql);
            repository.store(name, sql);
        }
        catch (SQLException e)
        {
            throw new DBIException("unable to prepare statement [" + sql + "] : " + e.getMessage(), e);
        }
    }

    PreparedStatement find(final String statement) throws DBIException
    {
        return findInternal(statement, new Arguments()
        {
            public Object[] objects()
            {
                return ParamTool.EMPTY_OBJECT_ARRAY;
            }
        });
    }

    PreparedStatement find(final String statement, final Map params) throws DBIException
    {
        return findInternal(statement, new Arguments()
        {
            public Object[] objects()
            {
                return ParamTool.getParamsFromMap(parametersFor(statement), params);
            }
        });
    }

    PreparedStatement find(final String statement, final Collection args) throws DBIException
    {
        return findInternal(statement, new Arguments()
        {
            public Object[] objects()
            {
                return ParamTool.getParamsFromCollection(args);
            }
        });
    }

    PreparedStatement find(final String statement, final Object bean) throws DBIException
    {
        return findInternal(statement, new Arguments()
        {
            public Object[] objects()
            {
                final String[] param_names = parametersFor(statement);
                return ParamTool.getParamsForBean(param_names, bean);
            }
        });
    }

    PreparedStatement find(final String statement, final Object[] params) throws DBIException
    {
        return this.findInternal(statement, new Arguments()
        {
            public Object[] objects()
            {
                return params;
            }
        });
    }

    private PreparedStatement findInternal(String statement, Arguments args) throws DBIException
    {
        final StatementEnvelope envelope = this.locateStatement(statement);
        PreparedStatement stmt;
        try
        {
            stmt = envelope.prepare(args);
        }
        catch (SQLException e)
        {
            throw new DBIException("exception binding argument to statement [" +
                                   statement + "] : " + e.getMessage(), e);
        }
        return stmt;
    }

    private StatementEnvelope store(final String name, final String sql) throws SQLException
    {
        final StatementEnvelope envelope = StatementFactory.build(conn, sql);
        envelopes.put(name, envelope);
        return envelope;
    }

    String[] parametersFor(String statement)
    {
        final StatementEnvelope envelope = (StatementEnvelope) envelopes.get(statement);
        return envelope.getNamedParameters();
    }
}
