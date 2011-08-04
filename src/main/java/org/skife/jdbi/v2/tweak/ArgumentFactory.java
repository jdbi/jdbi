package org.skife.jdbi.v2.tweak;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.Argument;

public interface ArgumentFactory<T>
{
    boolean accepts(Class<? super T> expectedType, T value, StatementContext ctx);

    Argument build(Class<? super T> expectedType, T value, StatementContext ctx);
}
