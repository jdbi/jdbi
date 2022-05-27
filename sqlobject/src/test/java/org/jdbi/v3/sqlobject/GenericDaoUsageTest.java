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

import java.util.Objects;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class GenericDaoUsageTest {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin());

    @BeforeEach
    public void setUp() {
        Handle handle = h2Extension.getSharedHandle();
        handle.execute("CREATE TABLE usermodel (id identity primary key, name VARCHAR)");
        handle.attach(UserDao.class).insert(new UserModel(1));
    }

    @Test
    public void testSqlObjectUseGenericDaoInterface() {
        Dao<UserModel, Integer> dao = h2Extension.getJdbi().onDemand(UserDao.class); // widening conversion
        assertThat(dao.getById(1)).isEqualTo(new UserModel(1));
    }

    @Test
    public void testSqlObjectUseConcreteDaoInterface() {
        UserDao userDao = h2Extension.getJdbi().onDemand(UserDao.class);
        UserModel entity = userDao.getById(1);
        assertThat(entity).isEqualTo(new UserModel(1));
    }

    public interface Model<PK> {
        PK getId();

        void setId(PK id);
    }

    public static class UserModel implements Model<Integer> {
        private int id;

        public UserModel() {}

        public UserModel(int id) {
            this.id = id;
        }

        @Override
        public Integer getId() {
            return id;
        }

        @Override
        public void setId(Integer id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            UserModel userModel = (UserModel) o;
            return id == userModel.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    public interface Dao<T extends Model<PK>, PK> extends SqlObject {
        T getById(PK id);

        Integer insert(T entity);
    }

    @RegisterBeanMapper(UserModel.class)
    public interface UserDao extends Dao<UserModel, Integer> {
        @Override
        @SqlQuery("SELECT * FROM UserModel WHERE id = :id")
        UserModel getById(Integer id);

        @Override
        @SqlUpdate("INSERT INTO UserModel (id) VALUES(:id)")
        @GetGeneratedKeys
        Integer insert(@BindBean UserModel user);
    }

}

