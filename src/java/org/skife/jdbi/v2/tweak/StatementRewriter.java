package org.skife.jdbi.v2.tweak;

/**
 * 
 */
public interface StatementRewriter
{
    String rewrite(String sql);
}
