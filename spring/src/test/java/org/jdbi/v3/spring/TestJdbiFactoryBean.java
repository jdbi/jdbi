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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.SqlStatements;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("/org/jdbi/v3/spring/test-context.xml")
@TestExecutionListeners(listeners = {DependencyInjectionTestExecutionListener.class})
public class TestJdbiFactoryBean {

    private Service service;
    private DataSource ds;
    private Jdbi jdbi;

    @Autowired
    public void setService(final Service service) {
        this.service = service;
    }

    @Autowired
    public void setDataSource(final DataSource dataSource) {
        this.ds = dataSource;
    }

    @Autowired
    public void setJdbi(final Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Test
    public void testServiceIsActuallySet() {
        assertThat(service).isNotNull();
    }

    @Test
    public void testFailsViaException() {
        assertThatExceptionOfType(ForceRollback.class).isThrownBy(() -> {
            service.inPropagationRequired(aJdbi -> {
                final Handle h = JdbiUtil.getHandle(aJdbi);
                final int count = h.execute("insert into something (id, name) values (7, 'ignored')");
                if (count == 1) {
                    throw new ForceRollback();
                } else {
                    throw new RuntimeException("!ZABAK");
                }
            });
        });

        try (Handle h = Jdbi.open(ds)) {
            final int count = h.createQuery("select count(*) from something").mapTo(int.class).one();
            assertThat(count).isZero();
        }
    }

    @Test
    public void testManualInstalledPlugin() {
        assertThat(jdbi.getConfig(SqlStatements.class).getAttribute(ManualPlugin.KEY))
                .isEqualTo(ManualPlugin.VALUE);
    }

    @Test
    public void testGlobalDefinedAttribute() {
        assertThat(jdbi.getConfig(SqlStatements.class).getAttribute("foo"))
                .isEqualTo("bar"); // see test-context.xml
    }

    @Test
    public void testNested() {
        assertThatExceptionOfType(ForceRollback.class).isThrownBy(() -> {
            service.inPropagationRequired(outer -> {
                final Handle h = JdbiUtil.getHandle(outer);
                h.execute("insert into something (id, name) values (7, 'ignored')");

                assertThatExceptionOfType(ForceRollback.class).isThrownBy(() -> {
                    service.inNested(inner -> {
                        final Handle h1 = JdbiUtil.getHandle(inner);
                        h1.execute("insert into something (id, name) values (8, 'ignored again')");

                        final int count = h1.createQuery("select count(*) from something").mapTo(Integer.class).one();
                        assertThat(count).isEqualTo(2);
                        throw new ForceRollback();
                    });
                });
                final int count = h.createQuery("select count(*) from something").mapTo(Integer.class).one();
                assertThat(count).isOne();
                throw new ForceRollback();
            });
        });
        service.inPropagationRequired(aJdbi -> {
            final Handle h = JdbiUtil.getHandle(aJdbi);
            final int count = h.createQuery("select count(*) from something").mapTo(Integer.class).one();
            assertThat(count).isZero();
        });
    }

    @Test
    public void testRequiresNew() {

        service.inPropagationRequired(outer -> {
            final Handle h = JdbiUtil.getHandle(outer);
            h.execute("insert into something (id, name) values (7, 'ignored')");

            assertThatExceptionOfType(ForceRollback.class).isThrownBy(() -> service.inRequiresNewReadUncommitted(inner -> {
                final Handle h1 = JdbiUtil.getHandle(inner);
                final int count = h1.createQuery("select count(*) from something").mapTo(Integer.class).one();
                assertThat(count).isOne();
                h1.execute("insert into something (id, name) values (8, 'ignored again')");
                throw new ForceRollback();
            }));

            final int count = h.createQuery("select count(*) from something").mapTo(Integer.class).one();
            assertThat(count).isOne();
        });
    }
}
