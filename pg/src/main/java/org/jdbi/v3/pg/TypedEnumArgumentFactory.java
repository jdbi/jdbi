package org.jdbi.v3.pg;

import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.v3.StatementContext;
import org.jdbi.v3.Types;
import org.jdbi.v3.tweak.Argument;
import org.jdbi.v3.tweak.ArgumentFactory;

/**
 * Default {@code jdbi} behavior is to bind {@code Enum} subclasses as
 * a string, which Postgres won't implicitly convert to an enum type.
 * If instead you bind it as {@code java.sql.Types.OTHER}, Postgres will
 * autodetect the enum correctly.
 */
public class TypedEnumArgumentFactory implements ArgumentFactory {
    @Override
    public Optional<Argument> build(Type type, Object value, StatementContext ctx) {
        if (!Types.getErasedType(type).isEnum()) {
            return Optional.empty();
        }
        return Optional.of((p, s, c) -> s.setObject(p, value, java.sql.Types.OTHER));
    }
}
