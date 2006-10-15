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
     * Use a non-standard StatementLocator to look up named statements for all
     * handles created from this DBi instance.
     *
     * @param locator StatementLocator which will be used by all Handle instances
     *                created from this DBI
     *
     * @see DBI#setStatementLocator(org.skife.jdbi.v2.tweak.StatementLocator)
     */
    void setStatementLocator(StatementLocator locator);

    /**
     * Use a non-standard StatementRewriter to transform SQL for all Handle instances
     * created by this DBI.
     *
     * @param rewriter StatementRewriter to use on all Handle instances
     *
     * @see DBI#setStatementRewriter(org.skife.jdbi.v2.tweak.StatementRewriter)
     */
    void setStatementRewriter(StatementRewriter rewriter);

    /**
     * Specify the TransactionHandler instance to use. This allows overriding
     * transaction semantics, or mapping into different transaction
     * management systems.
     * <p>
     * The default version uses local transactions on the database Connection
     * instances obtained.
     *
     * @param handler The TransactionHandler to use for all Handle instances obtained
     *                from this DBI
     *
     * @see DBI#setTransactionHandler(org.skife.jdbi.v2.tweak.TransactionHandler)
     */
    void setTransactionHandler(TransactionHandler handler);

    /**
     * Obtain a Handle to the data source wrapped by this DBI instance
     *
     * @return an open Handle instance
     * 
     * @see org.skife.jdbi.v2.DBI#open()
     */
    Handle open();
}
