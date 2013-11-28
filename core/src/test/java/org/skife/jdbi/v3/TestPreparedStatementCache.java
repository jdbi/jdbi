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
package org.skife.jdbi.v3;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;

import org.skife.jdbi.derby.Tools;
import org.skife.jdbi.v3.BasicHandle;
import org.skife.jdbi.v3.ClasspathStatementLocator;
import org.skife.jdbi.v3.ColonPrefixNamedParamStatementRewriter;
import org.skife.jdbi.v3.DefaultStatementBuilder;
import org.skife.jdbi.v3.DelegatingConnection;
import org.skife.jdbi.v3.Foreman;
import org.skife.jdbi.v3.MappingRegistry;
import org.skife.jdbi.v3.TimingCollector;
import org.skife.jdbi.v3.logging.NoOpLog;
import org.skife.jdbi.v3.tweak.transactions.LocalTransactionHandler;

public class TestPreparedStatementCache extends DBITestCase
{
    public void testSomething() throws Exception
    {
        final int[] prep_count = { 0 };

        Connection c = new DelegatingConnection(Tools.getConnection())
        {
            @Override
            public PreparedStatement prepareStatement(String s, int flag) throws SQLException
            {
                prep_count[0]++;
                return super.prepareStatement(s, flag);
            }

            @Override
            public PreparedStatement prepareStatement(String s) throws SQLException
            {
                prep_count[0]++;
                return super.prepareStatement(s);
            }
        };

        BasicHandle h = new BasicHandle(new LocalTransactionHandler(),
                                        new ClasspathStatementLocator(),
                                        new DefaultStatementBuilder(),
                                        new ColonPrefixNamedParamStatementRewriter(),
                                        c,
                                        new HashMap<String, Object>(),
                                        new NoOpLog(),
                                        TimingCollector.NOP_TIMING_COLLECTOR,
                                        new MappingRegistry(),
                                        new Foreman());

        h.createStatement("insert into something (id, name) values (:id, :name)")
                .bindFromProperties(new Something(0, "Keith"))
                .execute();

        assertEquals(1, prep_count[0]);

        h.createStatement("insert into something (id, name) values (:id, :name)")
                .bindFromProperties(new Something(0, "Keith"))
                .execute();

        assertEquals(1, prep_count[0]);
    }
}
