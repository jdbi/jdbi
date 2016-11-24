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

import static org.assertj.core.api.Assertions.assertThat;

import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.customizers.RegisterRowMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestObjectMethods
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        handle = db.getSharedHandle();
    }

    @Test
    public void testToString() throws Exception
    {
        DAO dao = handle.attach(DAO.class);
        assertThat(dao.toString()).contains(DAO.class.getName());
    }

    @Test
    public void testEquals() throws Exception
    {
        DAO dao = handle.attach(DAO.class);
        assertThat(dao).isEqualTo(dao);
    }

    @Test
    public void testNotEquals() throws Exception
    {
        DAO dao = handle.attach(DAO.class);
        DAO oad = handle.attach(DAO.class);
        assertThat(dao).isNotEqualTo(oad);
    }

    @Test
    public void testHashCodeDiff() throws Exception
    {
        DAO dao = handle.attach(DAO.class);
        DAO oad = handle.attach(DAO.class);
        assertThat(dao.hashCode()).isNotEqualTo(oad.hashCode());
    }

    @Test
    public void testHashCodeMatch() throws Exception
    {
        DAO dao = handle.attach(DAO.class);
        assertThat(dao.hashCode()).isEqualTo(dao.hashCode());
    }


    @RegisterRowMapper(SomethingMapper.class)
    public interface DAO
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") long id, @Bind("name") String name);
    }
}
