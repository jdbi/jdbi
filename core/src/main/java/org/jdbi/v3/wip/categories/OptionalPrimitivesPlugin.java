package org.jdbi.v3.wip.categories;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.spi.JdbiPlugin;

import static org.jdbi.v3.wip.categories.PluginUtils.registerClassArgument;
import static org.jdbi.v3.wip.categories.PluginUtils.registerClassMapper;
import static org.jdbi.v3.wip.categories.PluginUtils.registerGenericTypeArgument;
import static org.jdbi.v3.wip.categories.PluginUtils.registerGenericTypeMapper;

public class OptionalPrimitivesPlugin implements JdbiPlugin {
	@Override
	public void customizeJdbi(Jdbi jdbi) {
		registerOptionalArgument(jdbi, new GenericType<Optional<Boolean>>() {}, Types.BOOLEAN, PreparedStatement::setBoolean);
		registerOptionalArgument(jdbi, new GenericType<Optional<Byte>>() {}, Types.TINYINT, PreparedStatement::setByte);
		registerOptionalArgument(jdbi, new GenericType<Optional<Character>>() {}, Types.TINYINT, (p, i, v) -> p.setByte(i, (byte) (char) v));
		registerOptionalArgument(jdbi, new GenericType<Optional<Short>>() {}, Types.SMALLINT, PreparedStatement::setShort);
		registerOptionalArgument(jdbi, new GenericType<Optional<Integer>>() {}, Types.INTEGER, PreparedStatement::setInt);
		registerOptionalArgument(jdbi, new GenericType<Optional<Long>>() {}, Types.BIGINT, PreparedStatement::setLong);
		registerOptionalArgument(jdbi, new GenericType<Optional<Float>>() {}, Types.FLOAT, PreparedStatement::setFloat);
		registerOptionalArgument(jdbi, new GenericType<Optional<Double>>() {}, Types.DOUBLE, PreparedStatement::setDouble);

		registerClassArgument(jdbi, OptionalInt.class, Types.INTEGER, (p, i, v) -> {
			if (v.isPresent()) {
				p.setInt(i, v.getAsInt());
			} else {
				p.setNull(i, Types.INTEGER);
			}
		});
		registerClassArgument(jdbi, OptionalLong.class, Types.BIGINT, (p, i, v) -> {
			if (v.isPresent()) {
				p.setLong(i, v.getAsLong());
			} else {
				p.setNull(i, Types.BIGINT);
			}
		});
		registerClassArgument(jdbi, OptionalDouble.class, Types.DOUBLE, (p, i, v) -> {
			if (v.isPresent()) {
				p.setDouble(i, v.getAsDouble());
			} else {
				p.setNull(i, Types.DOUBLE);
			}
		});

		registerOptionalMapper(jdbi, new GenericType<Optional<Boolean>>() {}, ResultSet::getBoolean);
		registerOptionalMapper(jdbi, new GenericType<Optional<Byte>>() {}, ResultSet::getByte);
		registerOptionalMapper(jdbi, new GenericType<Optional<Character>>() {}, (r, i) -> (char) r.getByte(i));
		registerOptionalMapper(jdbi, new GenericType<Optional<Short>>() {}, ResultSet::getShort);
		registerOptionalMapper(jdbi, new GenericType<Optional<Integer>>() {}, ResultSet::getInt);
		registerOptionalMapper(jdbi, new GenericType<Optional<Long>>() {}, ResultSet::getLong);
		registerOptionalMapper(jdbi, new GenericType<Optional<Float>>() {}, ResultSet::getFloat);
		registerOptionalMapper(jdbi, new GenericType<Optional<Double>>() {}, ResultSet::getDouble);

		registerClassMapper(jdbi, OptionalInt.class, (r, i) -> r.getObject(i) == null ? OptionalInt.empty() : OptionalInt.of(r.getInt(i)));
		registerClassMapper(jdbi, OptionalLong.class, (r, i) -> r.getObject(i) == null ? OptionalLong.empty() : OptionalLong.of(r.getLong(i)));
		registerClassMapper(jdbi, OptionalDouble.class, (r, i) -> r.getObject(i) == null ? OptionalDouble.empty() : OptionalDouble.of(r.getDouble(i)));
	}

	private static <T> void registerOptionalArgument(Jdbi jdbi, GenericType<Optional<T>> genericType, int sqlType, PluginUtils.Binder<T> binder) {
		registerGenericTypeArgument(jdbi, genericType, sqlType, (p, i, v) -> {
			if (v.isPresent()) {
				binder.bind(p, i, v.get());
			} else {
				p.setNull(i, sqlType);
			}
		});
	}

	private static <T> void registerOptionalMapper(Jdbi jdbi, GenericType<Optional<T>> genericType, PluginUtils.ResultGetter<T> getter) {
		registerGenericTypeMapper(jdbi, genericType, (r, i) -> r.getObject(i) == null ? Optional.empty() : Optional.of(getter.get(r, i)));
	}
}
