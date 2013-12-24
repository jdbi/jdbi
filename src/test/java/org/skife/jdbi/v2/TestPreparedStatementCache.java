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
package org.skife.jdbi.v2;

import org.skife.jdbi.v2.logging.NoOpLog;
import org.skife.jdbi.v2.tweak.transactions.LocalTransactionHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;

public class TestPreparedStatementCache extends DBITestCase
{
    public void testSomething() throws Exception
    {
        final int[] prep_count = { 0 };

        Connection c = new DelegatingConnection(derbyHelper.getConnection())
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
        CachingStatementBuilder builder = new CachingStatementBuilder(new DefaultStatementBuilder());

        BasicHandle h = new BasicHandle(new LocalTransactionHandler(),
                                        new ClasspathStatementLocator(),
                                        builder,
                                        new ColonPrefixNamedParamStatementRewriter(),
                                        c,
                                        new HashMap<String, Object>(),
                                        new NoOpLog(),
                                        TimingCollector.NOP_TIMING_COLLECTOR,
                                        new MappingRegistry(),
                                        new Foreman(),
                                        new ContainerFactoryRegistry());

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
