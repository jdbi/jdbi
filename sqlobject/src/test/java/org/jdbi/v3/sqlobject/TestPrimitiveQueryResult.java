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
package org.jdbi.v3.sqlobject;

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestPrimitiveQueryResult
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    PrimitiveDao dao;

    @Before
    public void setUp() {
      dao = db.getSharedHandle().attach(PrimitiveDao.class);
      dao.insert(1, "foo");
    }

    @Test
    public void testBoolean() {
        assertThat(dao.getBoolean(1)).isTrue();
        assertThat(dao.getBoolean(2)).isFalse();
    }

    @Test
    public void testByte() {
        assertThat(dao.getByte(1)).isEqualTo((byte) 1);
        assertThat(dao.getByte(2)).isEqualTo((byte) 0);
    }

    @Test
    public void testChar() {
        assertThat(dao.getChar(1)).isEqualTo('a');
        assertThat(dao.getChar(2)).isEqualTo('\0');
    }

    @Test
    public void testShort() {
        assertThat(dao.getShort(1)).isEqualTo((short) 1);
        assertThat(dao.getShort(2)).isEqualTo((short) 0);
    }

    @Test
    public void testInt() {
        assertThat(dao.getInt(1)).isEqualTo(1);
        assertThat(dao.getInt(2)).isEqualTo(0);
    }

    @Test
    public void testLong() {
        assertThat(dao.getLong(1)).isEqualTo(1L);
        assertThat(dao.getLong(2)).isEqualTo(0L);
    }

    @Test
    public void testFloat() {
        assertThat(dao.getFloat(1)).isEqualTo(1f);
        assertThat(dao.getFloat(2)).isEqualTo(0f);
    }

    @Test
    public void testDouble() {
        assertThat(dao.getDouble(1)).isEqualTo(1d);
        assertThat(dao.getDouble(2)).isEqualTo(0d);
    }

    public interface PrimitiveDao {
        @SqlUpdate("insert into something(id, name) values (:id, :name)")
        void insert(int id, String name);

        @SqlQuery("select 1 from something where id = :id")
        boolean getBoolean(int id);

        @SqlQuery("select 1 from something where id = :id")
        byte getByte(int id);

        @SqlQuery("select 'a' from something where id = :id")
        char getChar(int id);

        @SqlQuery("select 1 from something where id = :id")
        short getShort(int id);

        @SqlQuery("select 1 from something where id = :id")
        int getInt(int id);

        @SqlQuery("select 1 from something where id = :id")
        long getLong(int id);

        @SqlQuery("select 1 from something where id = :id")
        float getFloat(int id);

        @SqlQuery("select 1 from something where id = :id")
        double getDouble(int id);
    }
}
