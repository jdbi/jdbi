package org.jdbi.v3.wip.categories;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Optional;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.wip.LoggableArgument;

import static org.jdbi.v3.wip.categories.PluginUtils.registerClassArgument;

public class PrimitivesPlugin implements JdbiPlugin {
	@Override
	public void customizeJdbi(Jdbi jdbi) {
		registerPrimitiveArgument(jdbi, boolean.class, PreparedStatement::setBoolean);
		registerPrimitiveArgument(jdbi, byte.class, PreparedStatement::setByte);
		registerPrimitiveArgument(jdbi, char.class, (p, i, v) -> p.setByte(i, (byte) (char) v));
		registerPrimitiveArgument(jdbi, short.class, PreparedStatement::setShort);
		registerPrimitiveArgument(jdbi, int.class, PreparedStatement::setInt);
		registerPrimitiveArgument(jdbi, long.class, PreparedStatement::setLong);
		registerPrimitiveArgument(jdbi, float.class, PreparedStatement::setFloat);
		registerPrimitiveArgument(jdbi, double.class, PreparedStatement::setDouble);

		registerClassArgument(jdbi, Boolean.class, Types.BOOLEAN, PreparedStatement::setBoolean);
		registerClassArgument(jdbi, Byte.class, Types.TINYINT, PreparedStatement::setByte);
		registerClassArgument(jdbi, Character.class, Types.TINYINT, (p, i, v) -> p.setByte(i, (byte) (char) v));
		registerClassArgument(jdbi, Short.class, Types.SMALLINT, PreparedStatement::setShort);
		registerClassArgument(jdbi, Integer.class, Types.INTEGER, PreparedStatement::setInt);
		registerClassArgument(jdbi, Long.class, Types.BIGINT, PreparedStatement::setLong);
		registerClassArgument(jdbi, Float.class, Types.FLOAT, PreparedStatement::setFloat);
		registerClassArgument(jdbi, Double.class, Types.DOUBLE, PreparedStatement::setDouble);

		registerPrimitiveMapper(jdbi, boolean.class, ResultSet::getBoolean);
		registerPrimitiveMapper(jdbi, byte.class, ResultSet::getByte);
		registerPrimitiveMapper(jdbi, char.class, (r, i) -> (char) r.getByte(i));
		registerPrimitiveMapper(jdbi, short.class, ResultSet::getShort);
		registerPrimitiveMapper(jdbi, int.class, ResultSet::getInt);
		registerPrimitiveMapper(jdbi, long.class, ResultSet::getLong);
		registerPrimitiveMapper(jdbi, float.class, ResultSet::getFloat);
		registerPrimitiveMapper(jdbi, double.class, ResultSet::getDouble);

		registerBoxedMapper(jdbi, Boolean.class, ResultSet::getBoolean);
		registerBoxedMapper(jdbi, Byte.class, ResultSet::getByte);
		registerBoxedMapper(jdbi, Character.class, (r, i) -> (char) r.getByte(i));
		registerBoxedMapper(jdbi, Short.class, ResultSet::getShort);
		registerBoxedMapper(jdbi, Integer.class, ResultSet::getInt);
		registerBoxedMapper(jdbi, Long.class, ResultSet::getLong);
		registerBoxedMapper(jdbi, Float.class, ResultSet::getFloat);
		registerBoxedMapper(jdbi, Double.class, ResultSet::getDouble);
	}

	private static <T> void registerPrimitiveArgument(Jdbi jdbi, Class<T> clazz, PluginUtils.Binder<T> binder) {
		jdbi.registerArgument((type, value, config) -> {
			if (clazz.equals(type)) {
				if (value == null) {
					throw new IllegalArgumentException("nulls should not be bound as primitives");
				} else {
					return Optional.of(new LoggableArgument(value, (position, statement, ctx) -> binder.bind(statement, position, (T) value)));
				}
			} else {
				return Optional.empty();
			}
		});
	}

	private static <T> void registerPrimitiveMapper(Jdbi jdbi, Class<T> clazz, PluginUtils.ResultGetter<T> getter) {
		PluginUtils.registerClassMapper(jdbi, clazz, (r, i) -> {
			if (r.getObject(i) == null) {
				throw new IllegalArgumentException("nulls cannot be mapped to primitives");
			} else {
				return getter.get(r, i);
			}
		});
	}

	private static <T> void registerBoxedMapper(Jdbi jdbi, Class<T> clazz, PluginUtils.ResultGetter<T> getter) {
		PluginUtils.registerClassMapper(jdbi, clazz, (r, i) -> {
			if (r.getObject(i) == null) {
				return null;
			} else {
				return getter.get(r, i);
			}
		});
	}
}
