package org.skife.jdbi.v2.tweak;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ExtractableResultSetMapper<T> {

	T extractByName(String name, ResultSet r) throws SQLException;
	
	T extractByIndex(int index, ResultSet r) throws SQLException;
	
}
