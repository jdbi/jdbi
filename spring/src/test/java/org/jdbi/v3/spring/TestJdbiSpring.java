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

import org.jdbi.v3.core.Jdbi;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for Integration between Template and Factory Bean
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestJdbiSpring.Config.class})
public class TestJdbiSpring {

    @Autowired
    private JdbiOperations jdbiOps;

    @Test
    public void testConfig() {
        assertThat(jdbiOps).isNotNull().isInstanceOf(JdbiTemplate.class);
        assertThat(((JdbiTemplate) jdbiOps).getJdbi()).isNotNull();
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
        public JdbiFactoryBean jdbi(DataSource dataSource) {
            return new JdbiFactoryBean(dataSource);
        }

        @Bean
        public JdbiOperations jdbiOperations(Jdbi jdbi) {
            return new JdbiTemplate(jdbi);
        }

        @Bean
        public PlatformTransactionManager transactionManager(Jdbi jdbi) {
            return new JdbiTransactionManager(jdbi);
        }
    }

}
