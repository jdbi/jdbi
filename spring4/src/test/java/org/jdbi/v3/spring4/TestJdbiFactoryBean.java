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
package org.jdbi.v3.spring4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import javax.sql.DataSource;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.SqlStatements;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/org/jdbi/v3/spring4/test-context.xml")
@TestExecutionListeners(listeners = {DependencyInjectionTestExecutionListener.class})
public class TestJdbiFactoryBean {

    private Service service;
    private DataSource ds;
    private Jdbi jdbi;

    @Autowired
    public void setService(Service service) {
        this.service = service;
    }

    @Autowired
    public void setDataSource(DataSource ds) {
        this.ds = ds;
    }

    @Autowired
    public void setJdbi(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Test
    public void testServiceIsActuallySet() throws Exception {
        assertThat(service).isNotNull();
    }

    @Test
    public void testFailsViaException() throws Exception {
        assertThatExceptionOfType(ForceRollback.class).isThrownBy(() -> {
            service.inPropagationRequired(jdbi -> {
                Handle h = JdbiUtil.getHandle(jdbi);
                final int count = h.execute("insert into something (id, name) values (7, 'ignored')");
                if (count == 1) {
                    throw new ForceRollback();
                } else {
                    throw new RuntimeException("!ZABAK");
                }
            });
        });

        try (final Handle h = Jdbi.open(ds)) {
            int count = h.createQuery("select count(*) from something").mapTo(int.class).findOnly();
            assertThat(count).isEqualTo(0);
        }
    }

    @Test
    public void testAutoInstalledPlugin() throws Exception {
        assertThat(jdbi.getConfig(SqlStatements.class).getAttribute(AutoPlugin.KEY))
                .isEqualTo(AutoPlugin.VALUE);
    }

    @Test
    public void testManualInstalledPlugin() throws Exception {
        assertThat(jdbi.getConfig(SqlStatements.class).getAttribute(ManualPlugin.KEY))
                .isEqualTo(ManualPlugin.VALUE);
    }

    @Test
    public void testGlobalDefinedAttribute() throws Exception {
        assertThat(jdbi.getConfig(SqlStatements.class).getAttribute("foo"))
                .isEqualTo("bar"); // see test-context.xml
    }

    @Test
    public void testNested() throws Exception {
        assertThatExceptionOfType(ForceRollback.class).isThrownBy(()->{
            service.inPropagationRequired(outer -> {
                final Handle h = JdbiUtil.getHandle(outer);
                h.execute("insert into something (id, name) values (7, 'ignored')");

                assertThatExceptionOfType(ForceRollback.class).isThrownBy(()-> {
                    service.inNested(inner -> {
                        final Handle h1 = JdbiUtil.getHandle(inner);
                        h1.execute("insert into something (id, name) values (8, 'ignored again')");

                        int count = h1.createQuery("select count(*) from something").mapTo(Integer.class).findOnly();
                        assertThat(count).isEqualTo(2);
                        throw new ForceRollback();
                    });
                });
                int count = h.createQuery("select count(*) from something").mapTo(Integer.class).findOnly();
                assertThat(count).isEqualTo(1);
                throw new ForceRollback();
            });
        });
        service.inPropagationRequired(jdbi -> {
            final Handle h = JdbiUtil.getHandle(jdbi);
            int count = h.createQuery("select count(*) from something").mapTo(Integer.class).findOnly();
            assertThat(count).isEqualTo(0);
        });
    }

    @Test
    public void testRequiresNew() throws Exception {

        service.inPropagationRequired(outer -> {
            final Handle h = JdbiUtil.getHandle(outer);
            h.execute("insert into something (id, name) values (7, 'ignored')");

            assertThatExceptionOfType(ForceRollback.class).isThrownBy(() -> {
                service.inRequiresNewReadUncommitted(inner -> {
                    final Handle h1 = JdbiUtil.getHandle(inner);
                    int count = h1.createQuery("select count(*) from something").mapTo(Integer.class).findOnly();
                    assertThat(count).isEqualTo(1);
                    h1.execute("insert into something (id, name) values (8, 'ignored again')");
                    throw new ForceRollback();
                });
            });

            int count = h.createQuery("select count(*) from something").mapTo(Integer.class).findOnly();
            assertThat(count).isEqualTo(1);
        });
    }
}
