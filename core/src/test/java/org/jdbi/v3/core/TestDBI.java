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

import org.jdbi.v3.core.exception.UnableToObtainConnectionException;
import org.junit.Rule;
import org.junit.Test;

public class TestDBI
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Test
    public void testDataSourceConstructor() throws Exception
    {
        Jdbi dbi = Jdbi.create(db.getConnectionString());
        try (Handle h = dbi.open()) {
            assertThat(h).isNotNull();
        }
    }

    @Test
    public void testConnectionFactoryCtor() throws Exception
    {
        Jdbi dbi = Jdbi.create(() -> {
            try
            {
                return DriverManager.getConnection(db.getConnectionString());
            }
            catch (SQLException e)
            {
                throw new UnableToObtainConnectionException(e);
            }
        });
        try (Handle h = dbi.open()) {
            assertThat(h).isNotNull();
        }
    }

    @Test(expected = UnableToObtainConnectionException.class)
    public void testCorrectExceptionOnSQLException() throws Exception
    {
        Jdbi dbi = Jdbi.create(() -> {
            throw new SQLException();
        });

        dbi.open();
    }

    @Test
    public void testWithHandle() throws Exception
    {
        Jdbi dbi = Jdbi.create(db.getConnectionString());
        String value = dbi.withHandle(handle -> {
            handle.insert("insert into something (id, name) values (1, 'Brian')");
            return handle.createQuery("select name from something where id = 1").mapToBean(Something.class).findOnly().getName();
        });
        assertThat(value).isEqualTo("Brian");
    }

    @Test
    public void testUseHandle() throws Exception
    {
        Jdbi dbi = Jdbi.create(db.getConnectionString());
        dbi.useHandle(handle -> {
            handle.insert("insert into something (id, name) values (1, 'Brian')");
            String value = handle.createQuery("select name from something where id = 1").mapToBean(Something.class).findOnly().getName();
            assertThat(value).isEqualTo("Brian");
        });
    }
}
