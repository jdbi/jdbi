package org.jdbi.v3.wip;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.statement.StatementContext;

public class LoggableArgument implements Argument {
	private final Object value;
	private final Argument argument;

	public LoggableArgument(Object value, Argument argument) {
		this.value = value;
		this.argument = argument;
	}

	@Override
	public void apply(int position, PreparedStatement statement, StatementContext context) throws SQLException {
		argument.apply(position, statement, context);
	}

	@Override
	public String toString() {
		return Objects.toString(value);
	}
}
