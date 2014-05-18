/*
 * Copyright (C) 2004 - 2013 Brian McCallister
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
package org.jdbi.v3;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jdbi.derby.Tools;
import org.jdbi.v3.logging.NoOpLog;
import org.jdbi.v3.tweak.StatementLocator;
import org.jdbi.v3.tweak.TransactionHandler;
import org.jdbi.v3.tweak.transactions.LocalTransactionHandler;

import junit.framework.TestCase;

/**
 *
 */
public abstract class DBITestCase extends TestCase
{
    protected final List<BasicHandle> handles = new ArrayList<BasicHandle>();
    private ExecutorService executor;

    @Override
    public void setUp() throws Exception
    {
        Tools.start();
        Tools.dropAndCreateSomething();
    }

    @Override
    public void tearDown() throws Exception
    {
        for (BasicHandle handle : handles)
        {
            handle.close();
        }
        Tools.stop();
    }

    protected StatementLocator getStatementLocator() {
        return new ClasspathStatementLocator();
    }

    protected BasicHandle openHandle() throws SQLException
    {
        Connection conn = Tools.getConnection();
        BasicHandle h = new BasicHandle(getTransactionHandler(),
                                        getStatementLocator(),
                                        new DefaultStatementBuilder(),
                                        new ColonPrefixNamedParamStatementRewriter(),
                                        conn,
                                        new HashMap<String, Object>(),
                                        new NoOpLog(),
                                        TimingCollector.NOP_TIMING_COLLECTOR,
                                        new MappingRegistry(),
                                        new Foreman());
        handles.add(h);
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
