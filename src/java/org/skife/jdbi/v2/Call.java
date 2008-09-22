package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.StatementLocator;
import org.skife.jdbi.v2.tweak.StatementRewriter;
import org.skife.jdbi.v2.tweak.StatementBuilder;
import org.skife.jdbi.v2.tweak.SQLLog;
import org.skife.jdbi.v2.tweak.StatementCustomizer;
import org.skife.jdbi.v2.tweak.Argument;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.CallableStatement;
import java.sql.Statement;
import java.sql.Types;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Used for invoking stored procedures.
 */
public class Call extends SQLStatement<Call>
{
	private final List<OutParamArgument> params = new ArrayList<OutParamArgument>();

	Call(Connection connection, StatementLocator locator, StatementRewriter statementRewriter, StatementBuilder cache, String sql, StatementContext ctx, SQLLog log)
	{
		super(new Binding(), locator, statementRewriter, connection, cache, sql, ctx, log);
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
			        out.map.put(param.position, obj);
			        if ( param.name != null ) {
				        out.map.put(param.name, obj);
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
				case Types.CLOB : case Types.VARCHAR : case Types.LONGNVARCHAR : case Types.LONGVARCHAR : case Types.NCLOB : case Types.NVARCHAR :
					return stmt.getString(position) ;
				case Types.BLOB : case Types.VARBINARY :
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

	public static class OutParameters
	{
		final Map map = new HashMap();

		public Object getObject(String name)
		{
			return map.get(name);
		}

		public Object getObject(Integer pos)
		{
			return map.get(pos);
		}

		public String getString(String name)
		{
			Object obj = map.get(name);
			if ( obj != null ) {
				return obj.toString();
			}
			return null;
		}

		public String getString(Integer pos)
		{
			Object obj = map.get(pos);
			if ( obj != null ) {
				return obj.toString();
			}
			return null;
		}

		public byte[] getBytes(String name)
		{
			Object obj = map.get(name);
			if ( obj instanceof byte[]) {
				return (byte[]) obj ;
			}
			return null;
		}

		public byte[] getBytes(Integer pos)
		{
			Object obj = map.get(pos);
			if ( obj instanceof byte[]) {
				return (byte[]) obj ;
			}
			return null;
		}

		public Integer getInt(String name)
		{
			Number n = getNumber(name) ;
			if ( n != null ) {
				return n.intValue();
			}
			return null;
		}

		public Integer getInt(Integer pos)
		{
			Number n = getNumber(pos) ;
			if ( n != null ) {
				return n.intValue();
			}
			return null;
		}

		public Long getLong(String name)
		{
			Number n = getNumber(name) ;
			if ( n != null ) {
				return n.longValue();
			}
			return null;
		}

		public Long getLong(Integer pos)
		{
			Number n = getNumber(pos) ;
			if ( n != null ) {
				return n.longValue();
			}
			return null;
		}

		public Short getShort(String name)
		{
			Number n = getNumber(name) ;
			if ( n != null ) {
				return n.shortValue();
			}
			return null;
		}

		public Short getShort(Integer pos)
		{
			Number n = getNumber(pos) ;
			if ( n != null ) {
				return n.shortValue();
			}
			return null;
		}

		public Date getDate(String name)
		{
			Long t = getEpoch(name) ;
			if ( t != null ) {
				return new Date(t);
			}
			return null;
		}

		public Date getDate(Integer pos)
		{
			Long t = getEpoch(pos) ;
			if ( t != null ) {
				return new Date(t);
			}
			return null;
		}

		public Timestamp getTimestamp(String name)
		{
			Long t = getEpoch(name) ;
			if ( t != null ) {
				return new Timestamp(t);
			}
			return null;
		}

		public Timestamp getTimestamp(Integer pos)
		{
			Long t = getEpoch(pos) ;
			if ( t != null ) {
				return new Timestamp(t);
			}
			return null;
		}

		public Double getDouble(String name)
		{
			Number n = getNumber(name) ;
			if ( n != null ) {
				return n.doubleValue();
			}
			return null;
		}

		public Double getDouble(Integer pos)
		{
			Number n = getNumber(pos) ;
			if ( n != null ) {
				return n.doubleValue();
			}
			return null;
		}

		public Float getFloat(String name)
		{
			Number n = getNumber(name) ;
			if ( n != null ) {
				return n.floatValue();
			}
			return null;
		}

		public Float getFloat(Integer pos)
		{
			Number n = getNumber(pos) ;
			if ( n != null ) {
				return n.floatValue();
			}
			return null;
		}

		private Number getNumber(Object name)
		{
			Object obj = map.get(name);
			if ( obj != null && obj instanceof Number) {
				return (Number) obj ;
			}
			return null;
		}

		private Long getEpoch(Object name)
		{
			Object obj = map.get(name) ;
			if ( obj != null && obj instanceof java.util.Date) {
				return ((java.util.Date) obj).getTime() ;
			}
			return null;
		}
	}
}
