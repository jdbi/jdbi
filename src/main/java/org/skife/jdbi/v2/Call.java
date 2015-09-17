/*
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

import org.skife.jdbi.v2.tweak.Argument;
import org.skife.jdbi.v2.tweak.SQLLog;
import org.skife.jdbi.v2.tweak.StatementBuilder;
import org.skife.jdbi.v2.tweak.StatementCustomizer;
import org.skife.jdbi.v2.tweak.StatementLocator;
import org.skife.jdbi.v2.tweak.StatementRewriter;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Used for invoking stored procedures.
 */
public class Call extends SQLStatement<Call>
{
    private final List<OutParamArgument> params = new ArrayList<OutParamArgument>();

    Call(Handle handle,
         StatementLocator locator,
         StatementRewriter rewriter,
         StatementBuilder cache,
         String sql,
         ConcreteStatementContext ctx,
         SQLLog log,
         TimingCollector timingCollector,
         Collection<StatementCustomizer> customizers,
         Foreman foreman,
         ContainerFactoryRegistry containerFactoryRegistry )
    {
        super(new Binding(), locator, rewriter, handle, cache, sql, ctx, log, timingCollector, customizers, foreman, containerFactoryRegistry);
    }

    /**
     * Register output parameter
     */
    public Call registerOutParameter(int position, int sqlType)
    {
        return registerOutParameter(position, sqlType, null);
    }

    public Call registerOutParameter(int position, int sqlType, CallableStatementMapper mapper)
    {
        getParams().addPositional(position, new OutParamArgument(sqlType, mapper, null));
        return this;
    }

    /**
     * Register output parameter
     */
    public Call registerOutParameter(String name, int sqlType)
    {
        return registerOutParameter(name, sqlType, null);
    }

    public Call registerOutParameter(String name, int sqlType, CallableStatementMapper mapper)
    {
        getParams().addNamed(name, new OutParamArgument(sqlType, mapper, name));
        return this;
    }

    /**
     * Invoke the callable statement
     */
    public OutParameters invoke()
    {
        try {
            return this.internalExecute(new QueryResultMunger<OutParameters>() {
                @Override
                public OutParameters munge(Statement results) throws SQLException
                {
                    OutParameters out = new OutParameters();
                    for ( OutParamArgument param : params ) {
                        Object obj = param.map((CallableStatement)results);
                        out.getMap().put(param.position, obj);
                        if ( param.name != null ) {
                            out.getMap().put(param.name, obj);
                        }
                    }
                    return out;
                }
            });
        }
        finally {
            cleanup();
        }
    }

    private class OutParamArgument implements Argument
    {
        private final int sqlType;
        private final CallableStatementMapper mapper;
        private final String name;
        private int position ;

        public OutParamArgument(int sqlType, CallableStatementMapper mapper, String name)
        {
            this.sqlType = sqlType;
            this.mapper = mapper;
            this.name = name;
            params.add(this);
        }

        @Override
        public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException
        {
            ((CallableStatement)statement).registerOutParameter(position, sqlType);
            this.position = position ;
        }

        public Object map(CallableStatement stmt) throws SQLException
        {
            if ( mapper != null ) {
                return mapper.map(position, stmt);
            }
            switch ( sqlType ) {
                case Types.CLOB : case Types.VARCHAR :
                case Types.LONGNVARCHAR :
                case Types.LONGVARCHAR :
                case Types.NCLOB :
                case Types.NVARCHAR :
                    return stmt.getString(position) ;
                case Types.BLOB :
                case Types.VARBINARY :
                    return stmt.getBytes(position) ;
                case Types.SMALLINT :
                    return stmt.getShort(position);
                case Types.INTEGER :
                    return stmt.getInt(position);
                case Types.BIGINT :
                    return stmt.getLong(position);
                case Types.TIMESTAMP : case Types.TIME :
                    return stmt.getTimestamp(position) ;
                case Types.DATE :
                    return stmt.getDate(position) ;
                case Types.FLOAT :
                    return stmt.getFloat(position);
                case Types.DECIMAL : case Types.DOUBLE :
                    return stmt.getDouble(position);
                default :
                    return stmt.getObject(position);

            }
        }
    }
}
