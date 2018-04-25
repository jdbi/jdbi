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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class GenericDaoUsageTest {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    @Before
    public void setUp() {
        Handle handle = dbRule.getSharedHandle();
        handle.execute("CREATE TABLE usermodel (id identity primary key, name VARCHAR)");
        handle.attach(UserDao.class).insert(new UserModel(1));
    }

    @Test
    public void testSqlObjectUseGenericDaoInterface() {
        // widening conversion
        Dao<UserModel, Integer> dao = dbRule.getJdbi().onDemand(UserDao.class);
        dao.getById(1);
    }

    @Test
    public void testSqlObjectUseConcreteDaoInterface() {
        UserDao userDao = dbRule.getJdbi().onDemand(UserDao.class);
        UserModel entity = userDao.getById(1);
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

