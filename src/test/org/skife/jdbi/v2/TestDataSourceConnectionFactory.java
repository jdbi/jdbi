/* Copyright 2004-2006 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.v2;

import junit.framework.TestCase;
import org.skife.jdbi.derby.Tools;

import java.sql.Connection;

public class TestDataSourceConnectionFactory extends TestCase
{
    private DataSourceConnectionFactory f;

    public void setUp() throws Exception
    {
        Tools.start();
        this.f = new DataSourceConnectionFactory(Tools.getDataSource());
    }

    public void tearDown() throws Exception
    {
        Tools.stop();
    }

    public void testObtainConnection() throws Exception
    {
        Connection c = f.openConnection();
        assertFalse(c.isClosed());
        c.close();
    }
}
