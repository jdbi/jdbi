/* Copyright 2004-2005 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.tweak;

import org.skife.jdbi.DBIException;
import org.skife.jdbi.Handle;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

/**
 * Transaction Handler which uses a JNDI lookup for an explicitely managed
 * JTA <code>UserTransaction</code> for all transactional activity.
 */
public class BMTTransactionHandler implements TransactionHandler
{
    private String jndi;

    /**
     * Must be passed JNDI lookup key for the UserTransaction in use
     */
    public BMTTransactionHandler(final String jndiUserTransaction)
    {
        this.jndi = jndiUserTransaction;
    }

    /**
     * Called when a transaction is started
     */
    public void begin(Handle handle)
    {
        InitialContext ctx = null;
        try
        {
            ctx = new InitialContext();
            final UserTransaction tx = (UserTransaction) ctx.lookup(jndi);
            tx.begin();
        }
        catch (Exception e)
        {
            throw new DBIException(e.getMessage(), e);
        }
        finally
        {
            if (ctx != null)
                try
                {
                    ctx.close();
                }
                catch (NamingException e)
                {
                    throw new DBIException(e.getMessage(), e);
                }
        }
    }

    /**
     * Called when a transaction is committed
     */
    public void commit(Handle handle)
    {
        InitialContext ctx = null;
        try
        {
            ctx = new InitialContext();
            final UserTransaction tx = (UserTransaction) ctx.lookup(jndi);
            tx.commit();
        }
        catch (Exception e)
        {
            throw new DBIException(e.getMessage(), e);
        }
        finally
        {
            if (ctx != null)
                try
                {
                    ctx.close();
                }
                catch (NamingException e)
                {
                    throw new DBIException(e.getMessage(), e);
                }
        }
    }

    /**
     * Called when a transaction is rolled back
     */
    public void rollback(Handle handle)
    {
        InitialContext ctx = null;
        try
        {
            ctx = new InitialContext();
            final UserTransaction tx = (UserTransaction) ctx.lookup(jndi);
            tx.rollback();
        }
        catch (Exception e)
        {
            throw new DBIException(e.getMessage(), e);
        }
        finally
        {
            if (ctx != null)
                try
                {
                    ctx.close();
                }
                catch (NamingException e)
                {
                    throw new DBIException(e.getMessage(), e);
                }
        }
    }

    /**
     * Called to test if a handle is in a transaction
     */
    public boolean isInTransaction(Handle handle)
    {
       InitialContext ctx = null;
        try
        {
            ctx = new InitialContext();
            final UserTransaction tx = (UserTransaction) ctx.lookup(jndi);
            return !(tx.getStatus() == Status.STATUS_NO_TRANSACTION);
        }
        catch (Exception e)
        {
            throw new DBIException(e.getMessage(), e);
        }
        finally
        {
            if (ctx != null)
                try
                {
                    ctx.close();
                }
                catch (NamingException e)
                {
                    throw new DBIException(e.getMessage(), e);
                }
        }
    }
}
