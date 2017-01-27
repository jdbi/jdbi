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
package jdbi.doc;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ResultsTest {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();
    private Handle handle;

    @Before
    public void getHandle() {
        handle = dbRule.getSharedHandle();
    }

    // tag::headlineExample[]
    public static class User {
        final int id;
        final String name;

        public User(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Before
    public void setUp() throws Exception {
        handle.execute("CREATE TABLE user (id INTEGER PRIMARY KEY AUTO_INCREMENT, name VARCHAR)");
        for (String name : Arrays.asList("Alice", "Bob", "Charlie", "Data")) {
            handle.execute("INSERT INTO user(name) VALUES (?)", name);
        }
    }

    @Test
    public void findBob() {
        User u = findUserById(2).orElseThrow(() -> new AssertionError("No user found"));
        assertThat(u.id).isEqualTo(2);
        assertThat(u.name).isEqualTo("Bob");
    }

    public Optional<User> findUserById(long id) {
        RowMapper<User> userMapper =
                (rs, ctx) -> new User(rs.getInt("id"), rs.getString("name"));
        return handle.createQuery("SELECT * FROM user WHERE id=:id")
            .bind("id", id)
            .map(userMapper)
            .findFirst();
    }
    // end::headlineExample[]

    // tag::rowMapper[]
    static final String SELECT_ALL_USERS = "SELECT * FROM user ORDER BY id ASC";

    static final class UserMapper implements RowMapper<User> {
        @Override
        public User map(ResultSet r, StatementContext ctx) throws SQLException {
            return new User(r.getInt("id"), r.getString("name"));
        }
    }

    @Test
    public void rowMapper() {
        List<User> users = handle.createQuery(SELECT_ALL_USERS)
            .map(new UserMapper())
            .list();

        assertThat(users).hasSize(4);
        assertThat(users.get(3).name).isEqualTo("Data");
    }
    // end::rowMapper[]

    // tag::rowMapperFactory[]
    @Test
    public void rowMapperFactory() {
        handle.registerRowMapper(new RowMapperFactory() {
            @Override
            public Optional<RowMapper<?>> build(Type type, ConfigRegistry config) {
                return type == User.class ?
                        Optional.of(new UserMapper()) :
                            Optional.empty();
            }
        });

        try (Stream<User> s = handle.createQuery(SELECT_ALL_USERS)
            .mapTo(User.class)
            .stream())
        {
            assertThat(s.filter(u -> u.id > 2)
                .map(u -> u.name)
                .findFirst()).contains("Charlie");
        }
    }
    // end::rowMapperFactory[]

    // tag::constructorMapper[]
    @Test
    public void constructorMapper() {
        handle.registerRowMapper(ConstructorMapper.of(User.class));
        Set<User> userSet = handle.createQuery(SELECT_ALL_USERS)
            .mapTo(User.class)
            .collect(Collectors.toSet());

        assertThat(userSet).hasSize(4);
    }
    // end::constructorMapper[]

    // tag::columnMapper[]
    public static class UserName {
        final String name;

        public UserName(String name) {
            this.name = name;
        }
    }

    public static class NamedUser {
        final int id;
        final UserName name;

        public NamedUser(int id, UserName name) {
            this.id = id;
            this.name = name;
        }
    }

    final ColumnMapperFactory userNameFactory = ColumnMapperFactory.of(UserName.class, (rs, index, cx) -> new UserName(rs.getString(index)));

    @Test
    public void columnMapper() {
        handle.registerColumnMapper(userNameFactory);
        handle.registerRowMapper(ConstructorMapper.of(NamedUser.class));

        NamedUser bob = handle.createQuery("SELECT id, name FROM user WHERE name = :name")
            .bind("name", "Bob")
            .mapTo(NamedUser.class)
            .findOnly();

        assertThat(bob.name.name).isEqualTo("Bob");
    }
    // end::columnMapper[]

    // tag::beanMapper[]
    public static class UserBean {
        private int id;
        private String name;

        public int getId() {
            return id;
        }
        public void setId(int id) {
            this.id = id;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    public void beanMapper() {
        UserBean charlie = handle.createQuery("SELECT id, name FROM user WHERE name = :name")
            .bind("name", "Charlie")
            .mapToBean(UserBean.class)
            .findOnly();

        assertThat(charlie.getName()).isEqualTo("Charlie");
    }
    // end::beanMapper[]
}
