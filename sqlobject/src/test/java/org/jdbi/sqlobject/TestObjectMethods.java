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
package org.jdbi.sqlobject;

import com.google.common.testing.EqualsTester;
import org.jdbi.core.Handle;
import org.jdbi.core.mapper.SomethingMapper;
import org.jdbi.sqlobject.config.RegisterRowMapper;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestObjectMethods {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
    }

    @Test
    public void testToString() {
        DAO dao = handle.attach(DAO.class);
        assertThat(dao.toString()).contains(DAO.class.getName());
    }

    @Test
    public void testEquals() {
        DAO dao = handle.attach(DAO.class);
        new EqualsTester().addEqualityGroup(dao, dao).testEquals();
    }

    @Test
    public void testNotEquals() {
        DAO dao = handle.attach(DAO.class);
        DAO oad = handle.attach(DAO.class);
        assertThat(dao).isNotEqualTo(oad);
    }

    @Test
    public void testHashCodeDiff() {
        DAO dao = handle.attach(DAO.class);
        DAO oad = handle.attach(DAO.class);
        assertThat(dao.hashCode()).isNotEqualTo(oad.hashCode());
    }

    @Test
    public void testHashCodeMatch() {
        DAO dao = handle.attach(DAO.class);
        assertThat(dao.hashCode()).isEqualTo(dao.hashCode());
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface DAO {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") long id, @Bind("name") String name);
    }
}
