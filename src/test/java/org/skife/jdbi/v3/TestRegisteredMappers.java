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
package org.skife.jdbi.v3;

import junit.framework.TestCase;

import org.h2.jdbcx.JdbcDataSource;
import org.skife.jdbi.v3.DBI;
import org.skife.jdbi.v3.Handle;
import org.skife.jdbi.v3.sqlobject.SomethingMapper;
import org.skife.jdbi.v3.tweak.HandleCallback;

import java.util.UUID;

public class TestRegisteredMappers extends TestCase
{
    private DBI dbi;
    private Handle handle;

    public void setUp() throws Exception
    {
        dbi = new DBI("jdbc:h2:mem:" + UUID.randomUUID());
        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");

    }

    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
    }


    public void testRegisterInferredOnDBI() throws Exception
    {
        dbi.registerMapper(new SomethingMapper());
        Something sam = dbi.withHandle(new HandleCallback<Something>()
        {

            public Something withHandle(Handle handle) throws Exception
            {
                handle.insert("insert into something (id, name) values (18, 'Sam')");

                return handle.createQuery("select id, name from something where id = :id")
                    .bind("id", 18)
                    .mapTo(Something.class)
                    .first();
            }
        });

        assertEquals("Sam", sam.getName());
    }
}
