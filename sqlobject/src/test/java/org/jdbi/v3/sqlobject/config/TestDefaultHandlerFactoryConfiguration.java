/*
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
package org.jdbi.v3.sqlobject.config;


import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.rewriter.HashPrefixStatementRewriter;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.*;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class TestDefaultHandlerFactoryConfiguration
{
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    private AtomicInteger invocationCounter = new AtomicInteger(0);

    @Before
    public void setUp() throws Exception
    {
        Jdbi db = dbRule.getJdbi();

        DefaultHandlerFactory defaultHandlerFactory = new DefaultHandlerFactory() {

            DefaultHandlerFactory delegate = new DefaultMethodHandlerFactory();
            @Override
            public Handler buildHandler(Class<?> sqlObjectType, Method method) {
                invocationCounter.incrementAndGet();
                return delegate.buildHandler(sqlObjectType, method);
            }

            @Override
            public boolean accepts(Class<?> sqlObjectType, Method method) {
                return delegate.accepts(sqlObjectType, method);
            }
        };

        // this is the default, but be explicit for sake of clarity in test
        db.configure(DefaultHandlerFactoryConfiguration.class, c -> c.setDefaultHandlerFactory(defaultHandlerFactory));
        handle = db.open();
    }

    @Test
    public void testFoo() throws Exception
    {
        SomethingDao h = handle.attach(SomethingDao.class);
        Something s = h.insertAndFind(new Something(1, "Joy"));
        assertThat(s.getName()).isEqualTo("Joy");
        assertThat(invocationCounter.get()).isEqualTo(1);
    }


    @UseStatementRewriter(HashPrefixStatementRewriter.class)
    @RegisterRowMapper(SomethingMapper.class)
    public interface SomethingDao
    {
        @SqlUpdate("insert into something (id, name) values (#id, #name)")
        void insert(@BindBean Something s);

        @SqlQuery("select id, name from something where id = #id")
        Something findById(@Bind("id") int id);

        @Transaction
        default Something insertAndFind(Something s) {
            insert(s);
            return findById(s.getId());
        }

    }

}
