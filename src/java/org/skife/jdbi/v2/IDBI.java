package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.StatementLocator;
import org.skife.jdbi.v2.tweak.StatementRewriter;
import org.skife.jdbi.v2.tweak.TransactionHandler;

/**
 * An interface for {@link DBI} instances for systems which like
 * to work with interfaces.
 */
public interface IDBI
{
    /**
     * @see DBI#setStatementLocator(org.skife.jdbi.v2.tweak.StatementLocator)
     */
    void setStatementLocator(StatementLocator locator);

    /**
     * @see DBI#setStatementRewriter(org.skife.jdbi.v2.tweak.StatementRewriter)
     */
    void setStatementRewriter(StatementRewriter rewriter);

    /**
     * @see DBI#setTransactionHandler(org.skife.jdbi.v2.tweak.TransactionHandler)
     */
    void setTransactionHandler(TransactionHandler handler);

    /**
     * @see org.skife.jdbi.v2.DBI#open() 
     */
    Handle open();
}
