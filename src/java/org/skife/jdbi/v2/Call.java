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

/**
 * Used for invoking stored procedures.
 */
public class Call<ReturnType> extends SQLStatement<Call<ReturnType>>
{
	final CallableStatementMapper<ReturnType> mapper ;
	ReturnType returnObj = null ;

	Call(Connection connection, StatementLocator locator, StatementRewriter statementRewriter, StatementBuilder cache, String sql, StatementContext ctx, SQLLog log, CallableStatementMapper<ReturnType> mapper)
	{
		super(new Binding(), locator, statementRewriter, connection, cache, sql, ctx, log);
		this.mapper = mapper;
		ctx.setAttribute(getClass().getName(), "true");
		this.addStatementCustomizer(new MyStatementCustomizer());
	}

	/**
	 * Register output parameter
	 * @param position
	 * @param sqlType
	 * @return
	 */
	public Call<ReturnType> registerOutParameter(int position, int sqlType)
	{
	    getParams().addPositional(position, new OutParamArgument(sqlType));
	    return this;
	}

	/**
	 * Register output parameter
	 * @param name
	 * @param sqlType
	 * @return
	 */
	public Call<ReturnType> registerOutParameter(String name, int sqlType)
	{
	    getParams().addNamed(name, new OutParamArgument(sqlType));
	    return this;
	}

	/**
	 * Invoke the callable statement
	 * @return
	 */
	public ReturnType invoke()
	{
        this.internalExecute(QueryPreperator.NO_OP, new QueryResultMunger<Void>(){
	        public Void munge(Statement results) throws SQLException
	        {
		        return null;
	        }
        }, QueryPostMungeCleanup.CLOSE_RESOURCES_QUIETLY);
		return returnObj ;
	}


	private class MyStatementCustomizer implements StatementCustomizer
	{
		public void beforeExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException
		{
		}

		public void afterExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException
		{
			returnObj = mapper.map((CallableStatement)stmt);
		}
	}

	private class OutParamArgument implements Argument
	{
		private final int sqlType;

		public OutParamArgument(int sqlType)
		{
			this.sqlType = sqlType;
		}

		public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException
		{
			((CallableStatement)statement).registerOutParameter(position, sqlType);
		}
	}
}
