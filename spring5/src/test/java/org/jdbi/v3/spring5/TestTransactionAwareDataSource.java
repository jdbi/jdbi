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
package org.jdbi.v3.spring5;

import javax.sql.DataSource;

import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class TestTransactionAwareDataSource {

    private Jdbi jdbi;
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + TestTransactionAwareDataSource.class.getSimpleName() + ";DB_CLOSE_DELAY=-1");
        jdbi = Jdbi.create(new TransactionAwareDataSourceProxy(dataSource));
        transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        jdbi.useHandle(handle -> handle.execute("create table something (id integer)"));
    }

    @AfterEach
    void tearDown() {
        jdbi.useHandle(handle -> handle.execute("drop table something"));
    }

    @Test
    void testCommit() {
        transactionTemplate.executeWithoutResult(status -> jdbi.useHandle(handle -> handle.execute("insert into something (id) values (1)")));

        assertThat(count()).isOne();
    }

    @Test
    void testRollback() {
        assertThatExceptionOfType(ForceRollback.class).isThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            jdbi.useHandle(handle -> handle.execute("insert into something (id) values (1)"));
            assertThat(count()).isOne();
            throw new ForceRollback();
        }));

        assertThat(count()).isZero();
    }

    private int count() {
        return jdbi.withHandle(handle -> handle.createQuery("select count(*) from something").mapTo(int.class).one());
    }
}
