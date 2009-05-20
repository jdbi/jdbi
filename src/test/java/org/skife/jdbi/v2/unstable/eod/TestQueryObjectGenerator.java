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

package org.skife.jdbi.v2.unstable.eod;

import org.skife.jdbi.derby.Tools;
import org.skife.jdbi.v2.DBITestCase;
import org.skife.jdbi.v2.Handle;

/**
 *
 */
public class TestQueryObjectGenerator extends DBITestCase
{
    public void testApiWhichTakesConnection() throws Exception
    {
        Handle h = openHandle();
        MyQueries qo = QueryObjectFactory.createQueryObject(MyQueries.class, h.getConnection());
        assertNotNull(qo);
    }

    public void testApiWhichTakesDatasource() throws Exception
    {
        final Handle h = openHandle();
        MyQueries qo = QueryObjectFactory.createQueryObject(MyQueries.class, Tools.getDataSource());
        assertNotNull(qo);
    }
}
