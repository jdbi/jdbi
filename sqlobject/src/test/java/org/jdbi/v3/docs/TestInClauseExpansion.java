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
package org.jdbi.v3.docs;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.List;

import com.google.common.collect.ImmutableSet;

import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.sqlobject.SqlQuery;
import org.jdbi.v3.unstable.BindIn;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestInClauseExpansion
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugins(); // Guava
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        handle = db.getSharedHandle();
    }

    @Test
    public void testInClauseExpansion() throws Exception
    {
        handle.execute("insert into something (name, id) values ('Brian', 1), ('Jeff', 2), ('Tom', 3)");

        DAO dao = handle.attach(DAO.class);

        assertEquals(ImmutableSet.of("Brian", "Jeff"), dao.findIdsForNames(asList(1, 2)));
    }

    public interface DAO
    {
        @SqlQuery("select name from something where id in (<names>)")
        ImmutableSet<String> findIdsForNames(@BindIn("names") List<Integer> names);
    }

}
