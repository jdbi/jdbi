package org.skife.jdbi.v2.tweak;

import org.skife.jdbi.v2.Parameters;

/**
 * 
 */
public interface StatementRewriter
{
    ReWrittenStatement rewrite(String sql, Parameters params);
}
