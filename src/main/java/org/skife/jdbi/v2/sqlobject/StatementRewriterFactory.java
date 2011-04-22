package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.StatementRewriter;

public interface StatementRewriterFactory
{
    public StatementRewriter create(StatementLocatorAnnotation anno, Class sqlObjectType, StatementContext ctx);
}
