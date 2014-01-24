/*
 * Copyright (C) 2004 - 2014 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.v2;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.skife.jdbi.derby.DerbyHelper;
import org.skife.jdbi.v2.logging.NoOpLog;
import org.skife.jdbi.v2.tweak.StatementLocator;
import org.skife.jdbi.v2.tweak.TransactionHandler;
import org.skife.jdbi.v2.tweak.transactions.LocalTransactionHandler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 */
public abstract class DBITestCase
{
    protected static final List<BasicHandle> HANDLES = new ArrayList<BasicHandle>();
    private ExecutorService executor;

    protected static final DerbyHelper DERBY_HELPER = new DerbyHelper();

    @BeforeClass
    public static void setUpClass() throws Exception
    {
        DERBY_HELPER.start();
    }

    @Before
    public final void setUp() throws Exception
    {
        DERBY_HELPER.dropAndCreateSomething();
        doSetUp();
    }

    protected void doSetUp() throws Exception
    {
    }

    @After
    public final void tearDown() throws Exception
    {
        doTearDown();
    }

    protected void doTearDown() throws Exception
    {
    }

    @AfterClass
    public static void tearDownClass() throws Exception
    {
        for (BasicHandle handle : HANDLES)
        {
            handle.close();
        }
        DERBY_HELPER.stop();
    }

    protected StatementLocator getStatementLocator()
    {
        return new ClasspathStatementLocator();
    }

    protected BasicHandle openHandle() throws SQLException
    {
        Connection conn = DERBY_HELPER.getConnection();
        BasicHandle h = new BasicHandle(getTransactionHandler(),
                                        getStatementLocator(),
                                        new CachingStatementBuilder(new DefaultStatementBuilder()),
                                        new ColonPrefixNamedParamStatementRewriter(),
                                        conn,
                                        new HashMap<String, Object>(),
                                        new NoOpLog(),
                                        TimingCollector.NOP_TIMING_COLLECTOR,
                                        new MappingRegistry(),
                                        new Foreman(),
                                        new ContainerFactoryRegistry());
        HANDLES.add(h);
        return h;
    }

    protected TransactionHandler getTransactionHandler()
    {
        return new LocalTransactionHandler();
    }

    protected <T> Future<T> run(Callable<T> it)
    {
        if (this.executor == null) {
            this.executor = Executors.newCachedThreadPool();
        }
        return executor.submit(it);
    }

}
