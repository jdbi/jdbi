/*
 * Copyright 2004-2007 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.StatementLocator;

/**
 *
 */
public class TestStatementContext extends DBITestCase
{

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
    }

    public void testFoo() throws Exception
    {
        Handle h = openHandle();
        h.setStatementLocator(new StatementLocator() {

            public String locate(String name, StatementContext ctx) throws Exception
            {
                return name.replaceAll("<table>", String.valueOf(ctx.getAttribute("table")));
            }
        });
        final int inserted = h.createStatement("insert into <table> (id, name) values (:id, :name)")
                .bind("id", 7)
                .bind("name", "Martin")
                .define("table", "something")
                .execute();
        assertEquals(1, inserted);

    }
}
