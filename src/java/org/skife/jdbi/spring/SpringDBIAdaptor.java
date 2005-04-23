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
package org.skife.jdbi.spring;

import org.skife.jdbi.DBIException;
import org.skife.jdbi.Handle;
import org.skife.jdbi.HandleCallback;
import org.skife.jdbi.IDBI;
import org.skife.jdbi.tweak.StatementLocator;
import org.skife.jdbi.tweak.TransactionHandler;
import org.springframework.aop.framework.ProxyFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Map;

class SpringDBIAdaptor implements IDBI
{
    private final IDBI real;
    private DataSource dataSource;

    SpringDBIAdaptor(IDBI real, DataSource dataSource)
    {
        this.real = real;
        this.dataSource = dataSource;
        real.setTransactionHandler(new SpringTransactionHandler(this));
    }

    public Handle open() throws DBIException
    {
        ProxyFactory pf = new ProxyFactory();
        pf.setProxyTargetClass(false);
        pf.setInterfaces(new Class[]{Handle.class});
        pf.setTarget(real.open());
        pf.addAdvice(new SQLExceptionTranslatingThrowsAdvice(dataSource));
        return (Handle) pf.getProxy();
    }

    public void open(HandleCallback callback) throws DBIException
    {
        final Handle bound = DBIUtils.getHandle(this);
        try
        {
            callback.withHandle(bound);
        }
        catch (Exception e)
        {
            throw new DBIException(e.getMessage(), e);
        }
        finally
        {
            DBIUtils.closeHandleIfNecessary(bound, this);
        }
    }

    public Map getNamedStatements()
    {
        return real.getNamedStatements();
    }

    public void name(String name, String statement) throws DBIException
    {
        real.name(name, statement);
    }

    public void load(String name) throws DBIException, IOException
    {
        real.load(name);
    }

    public void setTransactionHandler(TransactionHandler handler)
    {
        throw new UnsupportedOperationException("jDBI requires a special transaction handler in " +
                                                "Spring in order to participate in Spring's transaction " +
                                                "system, you are not allowed to override this. Sorry.");
    }

    /**
     * Specify a non-standard statement locator.
     *
     * @param locator used to find externalized sql
     */
    public void setStatementLocator(StatementLocator locator)
    {
        real.setStatementLocator(locator);
    }
}
