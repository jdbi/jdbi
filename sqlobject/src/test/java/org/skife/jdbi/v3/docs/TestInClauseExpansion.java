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
package org.skife.jdbi.v3.docs;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.ImmutableSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v3.DBI;
import org.skife.jdbi.v3.Handle;
import org.skife.jdbi.v3.logging.PrintStreamLog;
import org.skife.jdbi.v3.sqlobject.SqlObjectBuilder;
import org.skife.jdbi.v3.sqlobject.SqlQuery;
import org.skife.jdbi.v3.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v3.unstable.BindIn;

public class TestInClauseExpansion
{
    private DBI    dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        dbi = new DBI("jdbc:h2:mem:" + UUID.randomUUID());

        dbi.setSQLLog(new PrintStreamLog(System.out));
        handle = dbi.open();
        handle.execute("create table something( id integer primary key, name varchar(100) )");
    }

    @After
    public void tearDown() throws Exception
    {
        handle.close();
    }

    @Test
    public void testInClauseExpansion() throws Exception
    {
        handle.execute("insert into something (name, id) values ('Brian', 1), ('Jeff', 2), ('Tom', 3)");

        DAO dao = SqlObjectBuilder.attach(handle, DAO.class);

        assertEquals(ImmutableSet.of("Brian", "Jeff"), dao.findIdsForNames(asList(1, 2)));
    }

    @UseStringTemplate3StatementLocator
    public static interface DAO
    {
        @SqlQuery
        public Set<String> findIdsForNames(@BindIn("names") List<Integer> names);
    }
}
