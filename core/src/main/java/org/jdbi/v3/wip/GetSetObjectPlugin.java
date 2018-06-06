package org.jdbi.v3.wip;

import java.sql.Types;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.spi.JdbiPlugin;

public class GetSetObjectPlugin implements JdbiPlugin {
	public static final Optional<Argument> NULL = Optional.of((position, statement, ctx) -> statement.setNull(position, Types.NULL));

	private final Map<Class<?>, Optional<ColumnMapper<?>>> mapperCache = new ConcurrentHashMap<>();

	private final Set<Class<?>> supported;

	public GetSetObjectPlugin(Class<?> c, Class<?>... cs) {
		supported = new HashSet<>(1 + cs.length);
		supported.add(c);
		supported.addAll(Arrays.asList(cs));
	}

	@Override
	public void customizeJdbi(Jdbi jdbi) {
		jdbi.registerArgument((type, value, config) -> {
			if (type instanceof Class<?> && supported.contains(type)) {
				if (value == null) {
					return NULL;
				} else {
					return Optional.of(new LoggableArgument(value, (position, statement, ctx) -> statement.setObject(position, value)));
				}
			} else {
				return Optional.empty();
			}
		});

		jdbi.registerColumnMapper((type, config) -> {
			if (type instanceof Class<?> && supported.contains(type)) {
				Class<?> c = (Class<?>) type;
				return mapperCache.computeIfAbsent(c, c2 -> Optional.of((r, columnNumber, ctx) -> r.getObject(columnNumber, c2)));
			} else {
				return Optional.empty();
			}
		});
	}
}
