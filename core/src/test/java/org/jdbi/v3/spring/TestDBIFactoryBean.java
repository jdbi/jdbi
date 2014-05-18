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
package org.jdbi.v3.spring;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.spi.LoggingEvent;
import org.jdbi.v3.DBI;
import org.jdbi.v3.Handle;
import org.jdbi.v3.util.IntegerMapper;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

public class TestDBIFactoryBean extends AbstractDependencyInjectionSpringContextTests
{
    static {
        BasicConfigurator.configure(new AppenderSkeleton() {

            @Override
            protected void append(LoggingEvent event)
            {
            }

            @Override
            public boolean requiresLayout()
            {
                return false;
            }

            @Override
            public void close()
            {
            }
        });
    }

    protected Service service;
    protected DataSource ds;

    public void setService(Service service)
    {
        this.service = service;
    }

    public void setDataSource(DataSource ds) throws SQLException
    {
        this.ds = ds;
    }

    @Override
    protected String[] getConfigLocations()
    {
        return new String[]{"org/jdbi/v3/spring/test-context.xml"};
    }

    public void testServiceIsActuallySet() throws Exception
    {
        assertNotNull(service);
    }

    public void testFailsViaException() throws Exception
    {
        try {
            service.inPropagationRequired(new Callback()
            {
                @Override
                public void call(DBI dbi)
                {
                    Handle h = DBIUtil.getHandle(dbi);
                    final int count = h.insert("insert into something (id, name) values (7, 'ignored')");
                    if (count == 1) {
                        throw new ForceRollback();
                    }
                    else {
                        throw new RuntimeException("!ZABAK");
                    }
                }
            });
        }
        catch (ForceRollback e) {
            assertTrue(true);
        }
        catch (RuntimeException e) {
            e.printStackTrace();
            fail("unexpected exception");
        }

        final Handle h = DBI.open(ds);

        int count = h.createQuery("select count(*) from something").map(new IntegerMapper()).first();

        assertEquals(0, count);
        h.close();
    }

    public void testNested() throws Exception
    {
        try {
            service.inPropagationRequired(new Callback()
            {
                @Override
                public void call(DBI outer)
                {
                    final Handle h = DBIUtil.getHandle(outer);
                    h.insert("insert into something (id, name) values (7, 'ignored')");

                    try {
                        service.inNested(new Callback()
                        {
                            @Override
                            public void call(DBI inner)
                            {
                                final Handle h = DBIUtil.getHandle(inner);
                                h.insert("insert into something (id, name) values (8, 'ignored again')");

                                int count = h.createQuery("select count(*) from something").map(new IntegerMapper()).first();
                                assertEquals(2, count);
                                throw new ForceRollback();
                            }
                        });
                        fail("should have thrown an exception");
                    }
                    catch (ForceRollback e) {
                        assertTrue(true);
                    }
                    int count = h.createQuery("select count(*) from something").map(new IntegerMapper()).first();
                    assertEquals(1, count);
                    throw new ForceRollback();
                }
            });
            fail("should have thrown an exception");
        }
        catch (ForceRollback e) {
            assertTrue(true);
        }

        service.inPropagationRequired(new Callback()
        {
            @Override
            public void call(DBI dbi)
            {
                final Handle h = DBIUtil.getHandle(dbi);
                int count = h.createQuery("select count(*) from something").map(new IntegerMapper()).first();
                assertEquals(0, count);
            }
        });
    }

    public void testRequiresNew() throws Exception
    {
        service.inPropagationRequired(new Callback()
        {
            @Override
            public void call(DBI outer)
            {
                final Handle h = DBIUtil.getHandle(outer);
                h.insert("insert into something (id, name) values (7, 'ignored')");

                try {
                    service.inRequiresNewReadUncommitted(new Callback()
                    {
                        @Override
                        public void call(DBI inner)
                        {
                            final Handle h = DBIUtil.getHandle(inner);
                            int count = h.createQuery("select count(*) from something").map(new IntegerMapper()).first();
                            assertEquals(1, count);
                            h.insert("insert into something (id, name) values (8, 'ignored again')");
                            throw new ForceRollback();
                        }
                    });
                }
                catch (ForceRollback e) {
                    assertTrue(true);
                }

                int count = h.createQuery("select count(*) from something").map(new IntegerMapper()).first();
                assertEquals(1, count);
            }
        });
    }
}
