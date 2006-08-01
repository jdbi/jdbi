package org.skife.jdbi.v2.tweak;

/**
 * Used for named statement locating.
 */
public interface StatementLocator
{
    /**
     * Use this to map from a named statement to SQL. The SQL returned will be passed to
     * a StatementRewriter, so this can include stuff like named params and whatnot.
     *
     * @param name The name of the statement, as provided to a Handle
     * @return the SQL to execute, after it goes through a StatementRewriter
     * @throws Exception if anything goes wrong, jDBI will percolate expected exceptions
     */
    public String locate(String name) throws Exception;
}
