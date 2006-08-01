package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.StatementLocator;
import org.skife.jdbi.v2.tweak.StatementRewriter;
import org.skife.jdbi.v2.tweak.TransactionHandler;

/**
 * 
 */
public interface IDBI
{
    void setStatementLocator(StatementLocator locator);

    void setStatementRewriter(StatementRewriter rewriter);

    void setTransactionHandler(TransactionHandler handler);

    Handle open();
}
