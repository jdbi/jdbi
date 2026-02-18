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
package org.jdbi.freemarker;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import org.jdbi.core.Handle;
import org.jdbi.core.mapper.reflect.ConstructorMapper;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.customizer.BindBean;
import org.jdbi.sqlobject.customizer.Define;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestIssue2562 {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    private Handle handle;
    private UserSearchDao dao;
    private List<User> users = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
        handle.registerRowMapper(User.class, ConstructorMapper.of(User.class));
        handle.execute("CREATE TABLE users (user_id identity primary key, name varchar(64))");
        dao = handle.attach(UserSearchDao.class);

        for (int i = 0; i < 10; i++) {
            handle.execute("INSERT INTO users (user_id, name) VALUES (?, ?)", i, "name_" + i);
            users.add(dao.getById(i));
        }
    }

    @Test
    public void bindDefineTest() {
        UserPageToken token = new UserPageToken(Direction.BACKWARD, 6);

        List<User> result = dao.findAll(token, 10);
        assertThat(result).hasSize(3).containsExactly(users.get(9), users.get(8), users.get(7));
    }

    @Test
    public void bindDefineNullTest() {
        List<User> result = dao.findAll(null, 10);
        assertThat(result).hasSize(10).containsExactlyElementsOf(users);
    }


    public static final class User {
        private final int userId;
        private final String name;

        public User(int userId, String name) {
            this.userId = userId;
            this.name = name;
        }

        public int getUserId() {
            return userId;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", User.class.getSimpleName() + "[", "]")
                .add("userId=" + userId)
                .add("name='" + name + "'")
                .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            User user = (User) o;
            return userId == user.userId && Objects.equals(name, user.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, name);
        }
    }

    public enum Direction {
        FORWARD, BACKWARD
    }

    public static final class UserPageToken {

        Direction direction;
        int userId;

        public UserPageToken(Direction direction, int userId) {
            this.direction = direction;
            this.userId = userId;
        }

        public Direction getDirection() {
            return direction;
        }

        public int getUserId() {
            return userId;
        }
    }

    public interface UserSearchDao {
        @SqlQuery
        @UseFreemarkerEngine
        @UseFreemarkerSqlLocator
        List<User> findAll(@BindBean("pageToken") @Define UserPageToken pageToken, @Bind int limit);

        @SqlQuery("SELECT * FROM users WHERE user_id = :userId")
        User getById(int userId);
    }
}
