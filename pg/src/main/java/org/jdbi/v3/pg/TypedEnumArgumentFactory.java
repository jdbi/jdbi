package org.jdbi.v3.pg;

import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.v3.StatementContext;
import org.jdbi.v3.Types;
import org.jdbi.v3.tweak.Argument;
import org.jdbi.v3.tweak.ArgumentFactory;

public class TypedEnumArgumentFactory implements ArgumentFactory {
    @Override
    public Optional<Argument> build(Type type, Object value, StatementContext ctx) {
        if (!Types.getErasedType(type).isEnum()) {
            return Optional.empty();
        }
        return Optional.of((p, s, c) -> s.setObject(p, value, java.sql.Types.OTHER));
    }
}
