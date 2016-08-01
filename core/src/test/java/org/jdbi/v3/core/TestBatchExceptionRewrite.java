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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.sql.SQLException;

import org.jdbi.v3.core.exception.UnableToExecuteStatementException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class TestBatchExceptionRewrite
{
    @Rule
    public PGDatabaseRule db = new PGDatabaseRule();

    @Before
    public void createTable() {
        db.getDbi().useHandle(h -> h.execute("create table something ( id int primary key, name varchar(50), integerValue integer, intValue integer )"));
    }

    @Test
    public void testSimpleBatch() throws Exception
    {
        Batch b = db.openHandle().createBatch();
        b.add("insert into something (id, name) values (0, 'Keith')");
        b.add("insert into something (id, name) values (0, 'Keith')");
        try {
            b.execute();
            fail();
        } catch (UnableToExecuteStatementException e) {
            assertSuppressions(e.getCause());
        }
    }

    @Test
    public void testPreparedBatch() throws Exception
    {
        PreparedBatch b = db.openHandle().prepareBatch("insert into something (id, name) values (?,?)");
        b.add(0, "a");
        b.add(0, "a");
        try {
            b.execute();
            fail();
        } catch (UnableToExecuteStatementException e) {
            assertSuppressions(e.getCause());
        }
    }

    private void assertSuppressions(Throwable cause) {
        LoggerFactory.getLogger(TestBatchExceptionRewrite.class).info("exception", cause);
        SQLException e = (SQLException) cause;
        assertEquals(e.getNextException(), e.getSuppressed()[0]);
        assertNull(e.getNextException().getNextException());
        assertEquals(1, e.getSuppressed().length);
    }
}
