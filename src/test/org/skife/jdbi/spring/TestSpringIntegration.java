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

import junit.framework.TestCase;
import org.skife.jdbi.DBIException;
import org.skife.jdbi.Handle;
import org.skife.jdbi.HandleCallback;
import org.skife.jdbi.IDBI;
import org.skife.jdbi.Something;
import org.skife.jdbi.DBI;
import org.skife.jdbi.derby.Tools;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

import javax.sql.DataSource;
import java.util.Map;
import java.util.List;
import java.sql.SQLException;

public class TestSpringIntegration extends TestCase
{
    public static final String CTX_BASE = "src/test-etc/applicationContext.xml";
    private static ApplicationContext ctx;

    public void setUp() throws Exception
    {
        Tools.start();
        Tools.dropAndCreateSomething();
        if (ctx == null)
        {
            ctx = new FileSystemXmlApplicationContext(CTX_BASE);
        }
    }

    public void tearDown() throws Exception
    {
        Tools.stop();
    }

    public void testGetDataSource() throws Exception
    {
        DataSource ds = (DataSource) ctx.getBean("dataSource");
        assertNotNull(ds);
    }

    public void testGetDBI()
    {
        IDBI dbi = (IDBI) ctx.getBean("dbi");
        assertNotNull(dbi);
    }

    public void testDBIUtilsSameHandleInTx() throws Exception
    {
        final DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        final PlatformTransactionManager ptm = (PlatformTransactionManager) ctx.getBean("transactionManager");
        final TransactionStatus status = ptm.getTransaction(def);

        final IDBI dbi = (IDBI) ctx.getBean("dbi");

        Handle one = DBIUtils.getHandle(dbi);
        Handle two = DBIUtils.getHandle(dbi);

        assertSame(one, two);

        ptm.commit(status);
    }

    public void testSuspendAndResumeTx() throws Exception
    {
        final DefaultTransactionDefinition prop_req = new DefaultTransactionDefinition();
        prop_req.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        final PlatformTransactionManager ptm = (PlatformTransactionManager) ctx.getBean("transactionManager");
        final TransactionStatus prop_req_status = ptm.getTransaction(prop_req);

        final IDBI dbi = (IDBI) ctx.getBean("dbi");

        final Handle one = DBIUtils.getHandle(dbi);

        final DefaultTransactionDefinition req_new = new DefaultTransactionDefinition();
        req_new.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // REQUIRES_NEW inside PROPAGATION_REQUIRED
        final TransactionStatus req_new_status = ptm.getTransaction(req_new);

        final Handle two = DBIUtils.getHandle(dbi);
        assertNotSame(one, two);

        ptm.commit(req_new_status);
        ptm.commit(prop_req_status);
    }

    public void testHandleDecorator() throws Exception
    {
        final IDBI dbi = (IDBI) ctx.getBean("dbi");
        final Handle h = DBIUtils.getHandle(dbi);
        Tools.dropAndCreateSomething();
        h.execute("insert into something (id, name) values (:id, :name)", new Something(1, "one"));
        final Map row = h.first("select * from something");
        assertEquals("hello", row.get("wombat"));
        assertEquals("one", row.get("name"));
        h.close();
    }

    public void testCloseIfNecessaryNotNecesarry() throws Exception
    {
        final DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        final PlatformTransactionManager ptm = (PlatformTransactionManager) ctx.getBean("transactionManager");
        final TransactionStatus status = ptm.getTransaction(def);

        final IDBI dbi = (IDBI) ctx.getBean("dbi");

        Handle one = DBIUtils.getHandle(dbi);

        DBIUtils.closeHandleIfNecessary(one, dbi);
        assertTrue(one.isOpen());

        ptm.commit(status);
        assertFalse(one.isOpen());
    }

    public void testCloseIfNecessaryIsNeccessary() throws Exception
    {
        final IDBI dbi = (IDBI) ctx.getBean("dbi");

        Handle one = DBIUtils.getHandle(dbi);

        DBIUtils.closeHandleIfNecessary(one, dbi);
        assertFalse(one.isOpen());
    }

    public void testHandleCallbackUsesTxHandle() throws Exception
    {
        final DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        final PlatformTransactionManager ptm = (PlatformTransactionManager) ctx.getBean("transactionManager");
        final TransactionStatus status = ptm.getTransaction(def);

        final IDBI dbi = (IDBI) ctx.getBean("dbi");

        final Handle outer = DBIUtils.getHandle(dbi);

        dbi.open(new HandleCallback()
        {
            public void withHandle(Handle handle) throws Exception
            {
                assertSame(outer, handle);
            }
        });

        ptm.commit(status);
    }

    public void testExceptionInHandleCallbackRollsbackTx() throws Exception
    {
        final DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        final PlatformTransactionManager ptm = (PlatformTransactionManager) ctx.getBean("transactionManager");
        final TransactionStatus status = ptm.getTransaction(def);

        final IDBI dbi = (IDBI) ctx.getBean("dbi");

        try
        {
            dbi.open(new HandleCallback()
            {
                public void withHandle(Handle handle) throws Exception
                {
                    throw new UnsupportedOperationException("Not yet implemented!");
                }
            });
            fail("Should have thrown exception");
        }
        catch (DBIException e)
        {
            assertTrue(true);
        }

        ptm.rollback(status);
    }

    public void testRollbackOnException() throws Exception
    {
        final Service service = (Service) ctx.getBean("service");
        try
        {
            service.execute(new Service.Job()
            {
                public Object execute(IDBI dbi)
                {
                    final Handle h = DBIUtils.getHandle(dbi);
                    h.execute("insert into something (name, id) values (:name, :id)", new Something(1, "one"));
                    throw new NullPointerException();
                }
            });
            fail("should have excepted");
        }
        catch (Exception e)
        {
            assertTrue("go through here", true);
        }
        new DBI(Tools.CONN_STRING).open(new HandleCallback()
        {
            public void withHandle(Handle handle) throws Exception
            {
                final List all = handle.query("select * from something");
                assertEquals(0, all.size());
            }
        });
    }
}
