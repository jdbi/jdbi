package org.skife.jdbi.v2.tweak;

import org.skife.jdbi.v2.Binding;

/**
 * Use to provide arbitrary statement rewriting.
 */
public interface StatementRewriter
{
    /**
     * Munge up the SQL as desired. Responsible for figuring out ow to bind any
     * arguments in to the resultant prepared statement.
     *
     * @param sql The SQL to rewrite
     * @param params contains the arguments which have been bound to this statement.
     * @return somethign which can provde the actual SQL to prepare a statement from
     *         and which can bind the correct arguments to that prepared statement
     */
    RewrittenStatement rewrite(String sql, Binding params);
}
