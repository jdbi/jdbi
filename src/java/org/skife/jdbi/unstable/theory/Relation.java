/* Copyright 2004-2005 Brian McCallister
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
package org.skife.jdbi.unstable.theory;

import org.skife.jdbi.Handle;

import java.util.Iterator;
import java.util.List;

public class Relation
{
    private final Database db;
    private final String name;

    Relation(Database db, String name)
    {
        this.db = db;
        this.name = name;
    }

    public Iterator iterator()
    {
        Handle h = db.getHandle();
        List results = h.query("select * from " + name);
        h.close();
        return results.iterator();
    }
}
