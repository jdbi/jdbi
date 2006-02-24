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
package org.skife.jdbi;

import junit.framework.TestCase;

import java.util.Map;

import org.skife.jdbi.RowMap;

public class TestRowMap extends TestCase
{
    public void testLower() throws Exception
    {
        final RowMap map = new RowMap();
        map.put("WOMBAT", "value");
        assertEquals("value", map.get("wombat"));
    }

    public void testUpper() throws Exception
    {
        final RowMap map = new RowMap();
        map.put("wombat", "value");
        assertEquals("value", map.get("WOMBAT"));
    }

    public void testContainsUpper()
    {
        final Map map = new RowMap();
        map.put("wombat", "value");
        assertTrue(map.containsKey("WOMBAT"));
    }

    public void testContainsLower()
    {
        final Map map = new RowMap();
        map.put("WOMBAT", "value");
        assertTrue(map.containsKey("wombat"));
    }
}
