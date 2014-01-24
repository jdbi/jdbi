package org.skife.jdbi.v2.tweak;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ExtractableResultSetMapper<T> {

	T extractByName(ResultSet r, String name) throws SQLException;
	
	T extractByIndex(ResultSet r, int index) throws SQLException;
	
}
