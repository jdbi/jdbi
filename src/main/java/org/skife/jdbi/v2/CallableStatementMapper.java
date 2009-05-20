package org.skife.jdbi.v2;

import java.sql.CallableStatement;
import java.sql.SQLException;

public interface CallableStatementMapper
{
	public Object map(int position, CallableStatement stmt) throws SQLException;
}
