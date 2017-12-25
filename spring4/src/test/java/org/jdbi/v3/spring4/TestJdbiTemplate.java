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

import org.jdbi.v3.core.Jdbi;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestJdbiTemplate.Config.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TestJdbiTemplate {

    @Autowired
    private JdbiOperations jdbiOps;

    @Autowired
    private TemplateUsingService service;

    @Before
    public void createSchema() {
        jdbiOps.useHandle(h -> h.execute("create table if not exists something (id integer, name varchar(50), integerValue integer, intValue integer)"));
    }

    @Test
    public void testJdbiOps() {
        assertThat(jdbiOps).isNotNull().isInstanceOf(JdbiTemplate.class);
    }

    @Test
    public void testFailsViaException() {
        assertThatExceptionOfType(ForceRollback.class).isThrownBy(() -> {
            service.inPropagationRequired(h -> {
                final int count = h.execute("insert into something (id, name) values (7, 'ignored')");
                if (count == 1) {
                    throw new ForceRollback();
                } else {
                    throw new RuntimeException("!ZABAK");
                }
            });
        });

        final int count = jdbiOps.withHandle(h -> h.createQuery("select count(*) from something").mapTo(Integer.class).findOnly());
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void testNested() throws Exception {
        assertThatExceptionOfType(ForceRollback.class).isThrownBy(() -> {
            service.inPropagationRequired(outer -> {
                outer.execute("insert into something (id, name) values (7, 'ignored')");

                assertThatExceptionOfType(ForceRollback.class).isThrownBy(() -> {
                    service.inNested(inner -> {
                        inner.execute("insert into something (id, name) values (8, 'ignored again')");

                        int count = inner.createQuery("select count(*) from something").mapTo(Integer.class).findOnly();
                        assertThat(count).isEqualTo(2);
                        throw new ForceRollback();
                    });
                });
                int count = outer.createQuery("select count(*) from something").mapTo(Integer.class).findOnly();
                assertThat(count).isEqualTo(1);
                throw new ForceRollback();
            });
        });
        service.inPropagationRequired(h -> {
            int count = h.createQuery("select count(*) from something").mapTo(Integer.class).findOnly();
            assertThat(count).isEqualTo(0);
        });
    }

    @Test
    public void testRequiresNew() throws Exception {
        service.inPropagationRequired(outer -> {
            outer.execute("insert into something (id, name) values (7, 'ignored')");

            assertThatExceptionOfType(ForceRollback.class).isThrownBy(() -> {
                service.inRequiresNewReadUncommitted(inner -> {
                    int count = inner.createQuery("select count(*) from something").mapTo(Integer.class).findOnly();
                    assertThat(count).isEqualTo(1);
                    inner.execute("insert into something (id, name) values (8, 'ignored again')");
                    throw new ForceRollback();
                });
            });

            int count = outer.createQuery("select count(*) from something").mapTo(Integer.class).findOnly();
            assertThat(count).isEqualTo(1);
        });
    }

    @Configuration
    @EnableTransactionManagement
    public static class Config {

        @Bean
        public DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2)
                    .build();
        }

        @Bean
        public Jdbi jdbi(DataSource dataSource) {
            return Jdbi.create(dataSource);
        }

        @Bean
        public JdbiOperations jdbiOperations(Jdbi jdbi) {
            return new JdbiTemplate(jdbi);
        }

        @Bean
        public PlatformTransactionManager transactionManager(Jdbi jdbi) {
            return new JdbiTransactionManager(jdbi);
        }

        @Bean
        public TemplateUsingService templateUsingService(JdbiOperations operations) {
            return new TemplateUsingService(operations);
        }
    }

}
