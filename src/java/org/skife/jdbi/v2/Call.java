package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.Argument;
import org.skife.jdbi.v2.tweak.SQLLog;
import org.skife.jdbi.v2.tweak.StatementBuilder;
import org.skife.jdbi.v2.tweak.StatementLocator;
import org.skife.jdbi.v2.tweak.StatementRewriter;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Represents output from a callable statement
     */
	public static class OutParameters
	{
		private final Map map = new HashMap();

		public Object getObject(String name)
		{
			return map.get(name);
		}

		public Object getObject(int pos)
		{
			return map.get(pos);
		}

		public String getString(String name)
		{
			Object obj = map.get(name);
			if ( obj != null ) {
				return obj.toString();
			}
			throw new IllegalArgumentException(String.format("Parameter %s does not exist", name));
		}

		public String getString(int pos)
		{
			Object obj = map.get(pos);
			if ( obj != null ) {
				return obj.toString();
			}
			throw new IllegalArgumentException(String.format("Parameter at %d does not exist", pos));
		}

		public byte[] getBytes(String name)
		{
			Object obj = map.get(name);
			if ( obj == null ) {
				throw new IllegalArgumentException(String.format("Parameter %s does not exist", name));
			}
			if (obj instanceof byte[]) {
				return (byte[]) obj ;
			}
			else {
				throw new IllegalArgumentException(String.format("Parameter %s is not byte[] but %s", name, obj.getClass()));
			}
		}

		public byte[] getBytes(int pos)
		{
			Object obj = map.get(pos);
			if ( obj == null ) {
				throw new IllegalArgumentException(String.format("Parameter at %d does not exist", pos));
			}
			if (obj instanceof byte[]) {
				return (byte[]) obj ;
			}
			else {
				throw new IllegalArgumentException(String.format("Parameter at %d is not byte[] but %s", pos, obj.getClass()));
			}
		}

		public Integer getInt(String name)
		{
			return getNumber(name).intValue() ;
		}

		public Integer getInt(int pos)
		{
			return getNumber(pos).intValue() ;
		}

		public Long getLong(String name)
		{
			return getNumber(name).longValue() ;
		}

		public Long getLong(int pos)
		{
			return getNumber(pos).longValue() ;
		}

		public Short getShort(String name)
		{
			return getNumber(name).shortValue() ;
		}

		public Short getShort(int pos)
		{
			return getNumber(pos).shortValue() ;
		}

		public Date getDate(String name)
		{
			return new Date(getEpoch(name));
		}

		public Date getDate(int pos)
		{
			return new Date(getEpoch(pos));
		}

		public Timestamp getTimestamp(String name)
		{
			return new Timestamp(getEpoch(name)) ;
		}

		public Timestamp getTimestamp(int pos)
		{
			return new Timestamp(getEpoch(pos)) ;
		}

		public Double getDouble(String name)
		{
			return getNumber(name).doubleValue() ;
		}

		public Double getDouble(int pos)
		{
			return getNumber(pos).doubleValue();
		}

		public Float getFloat(String name)
		{
			return getNumber(name).floatValue();
		}

		public Float getFloat(int pos)
		{
			return getNumber(pos).floatValue();
		}

		private Number getNumber(String name)
		{
			Object obj = map.get(name);
			if ( obj == null ) {
				throw new IllegalArgumentException(String.format("Parameter %s does not exist", name));
			}
			if (obj instanceof Number) {
				return (Number) obj ;
			}
			else {
				throw new IllegalArgumentException(String.format("Parameter %s is not a number but %s", name, obj.getClass()));
			}
		}

		private Number getNumber(int pos)
		{
			Object obj = map.get(pos);
			if ( obj == null ) {
				throw new IllegalArgumentException(String.format("Parameter at %d does not exist", pos));
			}
			if (obj instanceof Number) {
				return (Number) obj ;
			}
			else {
				throw new IllegalArgumentException(String.format("Parameter at %d is not a number but %s", pos, obj.getClass()));
			}
		}

		private Long getEpoch(String name)
		{
			Object obj = map.get(name) ;
			if ( obj == null ) {
				throw new IllegalArgumentException(String.format("Parameter %s does not exist", name));
			}
			if ( obj instanceof java.util.Date) {
				return ((java.util.Date) obj).getTime() ;
			}
			else {
				throw new IllegalArgumentException(String.format("Parameter %s is not Date but %s", name, obj.getClass()));
			}
		}

		private Long getEpoch(int pos)
		{
			Object obj = map.get(pos) ;
			if ( obj == null ) {
				throw new IllegalArgumentException(String.format("Parameter at %d does not exist", pos));
			}
			if ( obj instanceof java.util.Date) {
				return ((java.util.Date) obj).getTime() ;
			}
			else {
				throw new IllegalArgumentException(String.format("Parameter at %d is not Date but %s", pos, obj.getClass()));
			}
		}
	}
}
