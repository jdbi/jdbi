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
package org.jdbi.v3.jpa;

import java.util.List;

import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.Handle;
import org.jdbi.v3.sqlobject.SqlObjectBuilder;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AnnoTest {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Test
    public void test() throws Exception {
        Something brian = new Something(1, "Brian", null);
        Something keith = new Something(2, "Keith", 7777777777777L);

        Handle h = db.getSharedHandle();
        h.execute("drop table something");
        h.execute("create table something (id int primary key, name varchar(100), value bigint)");

        SomethingDAO dao = SqlObjectBuilder.attach(h, SomethingDAO.class);
        dao.insert(brian);
        dao.insert(keith);

        List<Something> rs = h
                .createQuery("select * from something order by id")
                .map(AnnoMapper.get(Something.class))
                .list();

        assertEquals(rs.size(), 2);
        assertSomethingEquals(rs.get(0), brian);
        assertSomethingEquals(rs.get(1), keith);

        h.close();
    }

    public static void assertSomethingEquals(Something actual, Something expected) {
        assertEquals(actual.id(), expected.id());
        assertEquals(actual.name(), expected.name());
        assertEquals(actual.value, expected.value);
    }
}
