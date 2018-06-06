package org.jdbi.v3.wip.categories;

import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.wip.LoggableArgument;

class PluginUtils {
	static <T> void registerGenericTypeArgument(Jdbi jdbi, GenericType<T> genericType, int sqlType, Binder<T> binder) {
		registerTypeArgument(jdbi, genericType.getType(), sqlType, binder);
	}

	static <T> void registerClassArgument(Jdbi jdbi, Class<T> clazz, int sqlType, Binder<T> binder) {
		registerTypeArgument(jdbi, clazz, sqlType, binder);
	}

	static <T> void registerTypeArgument(Jdbi jdbi, Type t, int sqlType, Binder<T> binder) {
		jdbi.registerArgument((type, value, config) -> {
			if (t.equals(type)) {
				if (value == null) {
					return Optional.of(new LoggableArgument(null, (position, statement, ctx) -> statement.setNull(position, sqlType)));
				} else {
					return Optional.of(new LoggableArgument(value, (position, statement, ctx) -> binder.bind(statement, position, (T) value)));
				}
			} else {
				return Optional.empty();
			}
		});
	}

	static <T> void registerGenericTypeMapper(Jdbi jdbi, GenericType<T> genericType, ResultGetter<T> getter) {
		registerTypeMapper(jdbi, genericType.getType(), getter);
	}

	static <T> void registerClassMapper(Jdbi jdbi, Class<T> clazz, ResultGetter<T> getter) {
		registerTypeMapper(jdbi, clazz, getter);
	}

	static <T> void registerTypeMapper(Jdbi jdbi, Type t, ResultGetter<T> getter) {
		jdbi.registerColumnMapper(t, (r, columnNumber, ctx) -> getter.get(r, columnNumber));
	}

	@FunctionalInterface
	interface Binder<T> {
		void bind(PreparedStatement p, int index, T value) throws SQLException;
	}

	@FunctionalInterface
	interface ResultGetter<T> {
		T get(ResultSet r, int i) throws SQLException;
	}
}
