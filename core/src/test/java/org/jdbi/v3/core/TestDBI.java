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
package org.jdbi.v3.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.sql.SQLException;

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Rule;
import org.junit.Test;

public class TestDBI
{
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    @Test
    public void testDataSourceConstructor() throws Exception
    {
        Jdbi db = Jdbi.create(this.dbRule.getConnectionString());
        try (Handle h = db.open()) {
            assertThat(h).isNotNull();
        }
    }

    @Test
    public void testConnectionFactoryCtor() throws Exception
    {
        Jdbi db = Jdbi.create(() -> {
            try
            {
                return DriverManager.getConnection(this.dbRule.getConnectionString());
            }
            catch (SQLException e)
            {
                throw new ConnectionException(e);
            }
        });
        try (Handle h = db.open()) {
            assertThat(h).isNotNull();
        }
    }

    @Test(expected = ConnectionException.class)
    public void testCorrectExceptionOnSQLException() throws Exception
    {
        Jdbi db = Jdbi.create(() -> {
            throw new SQLException();
        });

        db.open();
    }

    @Test
    public void testWithHandle() throws Exception
    {
        Jdbi db = Jdbi.create(this.dbRule.getConnectionString());
        String value = db.withHandle(handle -> {
            handle.insert("insert into something (id, name) values (1, 'Brian')");
            return handle.createQuery("select name from something where id = 1").mapToBean(Something.class).findOnly().getName();
        });
        assertThat(value).isEqualTo("Brian");
    }

    @Test
    public void testUseHandle() throws Exception
    {
        Jdbi db = Jdbi.create(this.dbRule.getConnectionString());
        db.useHandle(handle -> {
            handle.insert("insert into something (id, name) values (1, 'Brian')");
            String value = handle.createQuery("select name from something where id = 1").mapToBean(Something.class).findOnly().getName();
            assertThat(value).isEqualTo("Brian");
        });
    }
}
