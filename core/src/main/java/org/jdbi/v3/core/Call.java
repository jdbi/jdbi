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
package org.jdbi.v3.core;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.exception.UnableToExecuteStatementException;
import org.jdbi.v3.core.statement.StatementBuilder;
import org.jdbi.v3.core.statement.StatementCustomizer;

/**
 * Used for invoking stored procedures.
 */
public class Call extends SqlStatement<Call>
{
    private final Map<OutParam, Integer> paramColumns = new HashMap<>();

    Call(ConfigRegistry config,
         Handle handle,
         StatementBuilder cache,
         String sql,
         StatementContext ctx,
         Collection<StatementCustomizer> customizers)
    {
        super(config, new Binding(), handle, cache, sql, ctx, customizers);

        registerArgument(new OutParamArgument());
    }

    /**
     * Register a positional output parameter
     * @param position the parameter position (zero-based)
     * @param sqlType an SQL type constant as defined by {@link java.sql.Types} or by the JDBC vendor.
     * @return self
     */
    public Call registerOutParameter(int position, int sqlType)
    {
        return bind(position, new OutParam(sqlType));
    }

    /**
     * Register a positional output parameter
     * @param position the parameter position (zero-based)
     * @param sqlType an SQL type constant as defined by {@link java.sql.Types} or by the JDBC vendor.
     * @param mapper a mapper which converts the {@link CallableStatement} to a desired output type.
     * @return self
     */
    public Call registerOutParameter(int position, int sqlType, CallableStatementMapper mapper)
    {
        return bind(position, new OutParam(sqlType, mapper));
    }

    /**
     * Register a named output parameter
     * @param name the parameter name
     * @param sqlType an SQL type constant as defined by {@link java.sql.Types} or by the JDBC vendor.
     * @return self
     */
    public Call registerOutParameter(String name, int sqlType)
    {
        return bind(name, new OutParam(sqlType, null, name));
    }

    /**
     * Register a named output parameter
     * @param name the parameter name
     * @param sqlType an SQL type constant as defined by {@link java.sql.Types} or by the JDBC vendor.
     * @param mapper a mapper which converts the {@link CallableStatement} to a desired output type.
     * @return self
     */
    public Call registerOutParameter(String name, int sqlType, CallableStatementMapper mapper)
    {
        return bind(name, new OutParam(sqlType, mapper, name));
    }

    /**
     * Invoke the callable statement
     * @return the output parameters resulting from the invocation.
     */
    public OutParameters invoke()
    {
        try {
            final CallableStatement stmt = (CallableStatement) this.internalExecute();
            OutParameters out = new OutParameters();
            paramColumns.forEach((param, position) -> {
                Object obj = mapOutputParameter(stmt, position, param);
                out.getMap().put(position, obj);
                if (param.getName() != null) {
                    out.getMap().put(param.getName(), obj);
                }
            });
            return out;
        }
        finally {
            close();
        }
    }

    private Object mapOutputParameter(CallableStatement stmt, int position, OutParam param)
    {
        try {
            CallableStatementMapper mapper = param.getMapper();
            if ( mapper != null ) {
                return mapper.map(position, stmt);
            }
            switch (param.getSqlType()) {
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
        } catch (SQLException e) {
            throw new UnableToExecuteStatementException("Could not get OUT parameter from statement", e, getContext());
        }
    }

    private class OutParamArgument implements Argument<OutParam>
    {
        @Override
        public void apply(PreparedStatement statement, int position, OutParam param, StatementContext ctx) throws SQLException
        {
            paramColumns.put(param, position);
            ((CallableStatement)statement).registerOutParameter(position, param.getSqlType());
        }
    }

    private static class OutParam {
        private final int sqlType;
        private final CallableStatementMapper mapper;
        private final String name;

        public OutParam(int sqlType) {
            this(sqlType, null);
        }

        public OutParam(int sqlType, CallableStatementMapper mapper) {
            this(sqlType, mapper, null);
        }

        public OutParam(int sqlType, CallableStatementMapper mapper, String name) {
            this.sqlType = sqlType;
            this.mapper = mapper;
            this.name = name;
        }

        public int getSqlType() {
            return sqlType;
        }

        public CallableStatementMapper getMapper() {
            return mapper;
        }

        public String getName() {
            return name;
        }
    }
}
