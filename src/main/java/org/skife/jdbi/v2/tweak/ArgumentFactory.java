package org.skife.jdbi.v2.tweak;

import org.skife.jdbi.v2.StatementContext;

public interface ArgumentFactory<T>
{
    boolean accepts(Class<?> expectedType, Object value, StatementContext ctx);

    Argument build(Class<?> expectedType, T value, StatementContext ctx);
}
