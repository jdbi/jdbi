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
package org.jdbi.v3;

import org.jdbi.derby.Tools;
import org.jdbi.v3.DBI;
import org.jdbi.v3.Handle;
import org.jdbi.v3.StatementContext;
import org.jdbi.v3.TransactionCallback;
import org.jdbi.v3.TransactionStatus;
import org.jdbi.v3.tweak.HandleCallback;
import org.jdbi.v3.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 */
public class TestHandle extends DBITestCase
{
    public void testInTransaction() throws Exception
    {
        Handle h = this.openHandle();

        String value = h.inTransaction(new TransactionCallback<String>()
        {
            public String inTransaction(Handle handle, TransactionStatus status) throws Exception
            {
                handle.insert("insert into something (id, name) values (1, 'Brian')");
                return handle.createQuery("select name from something where id = 1").map(Something.class).first().getName();
            }
        });
        assertEquals("Brian", value);
    }

    public void testSillyNumberOfCallbacks() throws Exception
    {
        Handle h = openHandle();
        h.insert("insert into something (id, name) values (1, 'Keith')");
        h.close();


        String value = new DBI(Tools.CONN_STRING).withHandle(new HandleCallback<String>()
        {
            public String withHandle(Handle handle) throws Exception
            {
                return handle.inTransaction(new TransactionCallback<String>()
                {
                    public String inTransaction(Handle handle, TransactionStatus status) throws Exception
                    {
                        return handle.createQuery("select name from something where id = 1").map(new ResultSetMapper<String>()
                        {
                            public String map(int index, ResultSet r, StatementContext ctx) throws SQLException
                            {
                                return r.getString(1);
                            }
                        }).first();
                    }
                });
            }
        });

        assertEquals("Keith", value);
    }
}
