package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.Argument;

public interface ArgumentFactory<T>
{
    boolean accepts(Class<? super T> expectedType, T it, StatementContext ctx);

    Argument build(Class<? super T> expectedType, T it, StatementContext ctx);
}
