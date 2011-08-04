package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.Argument;

public interface ArgumentFactory
{
    boolean accepts(Class expectedType, Object it, StatementContext ctx);

    Argument build(Class expectedType, Object it, StatementContext ctx);
}
