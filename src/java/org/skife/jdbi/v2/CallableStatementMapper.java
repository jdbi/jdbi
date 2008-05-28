package org.skife.jdbi.v2;

import java.sql.CallableStatement;
import java.sql.SQLException;

public interface CallableStatementMapper<ReturnType>
{
	public ReturnType map(CallableStatement call) throws SQLException;
}
