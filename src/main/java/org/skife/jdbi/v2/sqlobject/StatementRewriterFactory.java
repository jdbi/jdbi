package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.StatementRewriter;

public interface StatementRewriterFactory
{
    public StatementRewriter create(SqlPreperationAnnotation anno, Class sqlObjectType, StatementContext ctx);
}
