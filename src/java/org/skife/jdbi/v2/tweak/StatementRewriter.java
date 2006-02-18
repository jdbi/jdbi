package org.skife.jdbi.v2.tweak;

/**
 * 
 */
public interface StatementRewriter
{
    ReWrittenStatement rewrite(String sql);
}
