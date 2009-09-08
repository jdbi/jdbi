package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.Argument;
import org.skife.jdbi.v2.tweak.SQLLog;
import org.skife.jdbi.v2.tweak.StatementBuilder;
import org.skife.jdbi.v2.tweak.StatementLocator;
import org.skife.jdbi.v2.tweak.StatementRewriter;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Used for invoking stored procedures.
 */
public class Call extends SQLStatement<Call>
{
	private final List<OutParamArgument> params = new ArrayList<OutParamArgument>();

	Call(Connection connection,
         StatementLocator locator,
         StatementRewriter statementRewriter,
         StatementBuilder cache,
         String sql,
         StatementContext ctx,
         SQLLog log,
         TimingCollector timingCollector)
	{
		super(new Binding(), locator, statementRewriter, connection, cache, sql, ctx, log, timingCollector);
	}

	/**
	 * Register output parameter
	 * @param position
	 * @param sqlType
	 * @return
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
	 * @param name
	 * @param sqlType
	 * @return
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
	 * @return
	 */
	public OutParameters invoke()
	{
        return this.internalExecute(QueryPreperator.NO_OP, new QueryResultMunger<OutParameters>(){
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
        }, QueryPostMungeCleanup.CLOSE_RESOURCES_QUIETLY);
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
