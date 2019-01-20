package org.jdbi.v3.core.internal.enums;

import java.sql.Types;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jdbi.v3.core.EnumByName;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.result.UnableToProduceResultException;
import org.jdbi.v3.core.statement.StatementContext;

@EnumByName
public class ByName implements EnumStrategy {
    private static final EnumStrategy INSTANCE = new ByName();

    public static EnumStrategy singleton() {
        return INSTANCE;
    }

    @Override
    public int nullType() {
        return Types.VARCHAR;
    }

    @Override
    public Optional<Argument> bind(Arguments args, Enum<?> value) {
        return args.findFor(String.class, value.name());
    }

    @Override
    public <E extends Enum<E>> ColumnMapper<E> getMapper(Class<E> enumClass) {
        final ConcurrentMap<String, Enum<?>> cache = new ConcurrentHashMap<>();
        return (rs, col, ctx) -> {
            String name = ctx.findColumnMapperFor(String.class)
                    .orElseThrow(() -> new UnableToProduceResultException("a String column mapper is required to map Enums from names", ctx))
                    .map(rs, col, ctx);

            return name == null ? null : enumClass.cast(cache.computeIfAbsent(name.toLowerCase(), lowercased -> matchByName(enumClass, name, ctx)));
        };
    }

    private static <E extends Enum<E>> E matchByName(Class<E> type, String name, StatementContext ctx) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException ignored) {
            return Arrays.stream(type.getEnumConstants())
                .filter(e -> e.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new UnableToProduceResultException(String.format(
                    "no %s value could be matched to the name %s", type.getSimpleName(), name
                ), ctx));
        }
    }
}
