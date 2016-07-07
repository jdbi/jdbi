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
package org.jdbi.v3.spring;

import javax.sql.DataSource;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Handle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/org/jdbi/v3/spring/test-context.xml")
@TestExecutionListeners(listeners = {DependencyInjectionTestExecutionListener.class})
public class TestDBIFactoryBean {

    private Service service;
    private DataSource ds;

    @Autowired
    public void setService(Service service)
    {
        this.service = service;
    }

    @Autowired
    public void setDataSource(DataSource ds)
    {
        this.ds = ds;
    }

    @Test
    public void testServiceIsActuallySet() throws Exception
    {
        assertNotNull(service);
    }

    @Test
    public void testFailsViaException() throws Exception
    {
        try {
            service.inPropagationRequired(dbi -> {
                Handle h = DBIUtil.getHandle(dbi);
                final int count = h.insert("insert into something (id, name) values (7, 'ignored')");
                if (count == 1) {
                    throw new ForceRollback();
                }
                else {
                    throw new RuntimeException("!ZABAK");
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

        try (final Handle h = Jdbi.open(ds)) {
            int count = h.createQuery("select count(*) from something").mapTo(int.class).findOnly();
            assertEquals(0, count);
        }
    }

    @Test
    public void testNested() throws Exception
    {
        try {
            service.inPropagationRequired(outer -> {
                final Handle h = DBIUtil.getHandle(outer);
                h.insert("insert into something (id, name) values (7, 'ignored')");

                try {
                    service.inNested(inner -> {
                        final Handle h1 = DBIUtil.getHandle(inner);
                        h1.insert("insert into something (id, name) values (8, 'ignored again')");

                        int count = h1.createQuery("select count(*) from something").mapTo(Integer.class).findOnly();
                        assertEquals(2, count);
                        throw new ForceRollback();
                    });
                    fail("should have thrown an exception");
                }
                catch (ForceRollback e) {
                    assertTrue(true);
                }
                int count = h.createQuery("select count(*) from something").mapTo(Integer.class).findOnly();
                assertEquals(1, count);
                throw new ForceRollback();
            });
            fail("should have thrown an exception");
        }
        catch (ForceRollback e) {
            assertTrue(true);
        }

        service.inPropagationRequired(dbi -> {
            final Handle h = DBIUtil.getHandle(dbi);
            int count = h.createQuery("select count(*) from something").mapTo(Integer.class).findOnly();
            assertEquals(0, count);
        });
    }

    @Test
    public void testRequiresNew() throws Exception
    {
        service.inPropagationRequired(outer -> {
            final Handle h = DBIUtil.getHandle(outer);
            h.insert("insert into something (id, name) values (7, 'ignored')");

            try {
                service.inRequiresNewReadUncommitted(inner -> {
                    final Handle h1 = DBIUtil.getHandle(inner);
                    int count = h1.createQuery("select count(*) from something").mapTo(Integer.class).findOnly();
                    assertEquals(1, count);
                    h1.insert("insert into something (id, name) values (8, 'ignored again')");
                    throw new ForceRollback();
                });
            }
            catch (ForceRollback e) {
                assertTrue(true);
            }

            int count = h.createQuery("select count(*) from something").mapTo(Integer.class).findOnly();
            assertEquals(1, count);
        });
    }
}
