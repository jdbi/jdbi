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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.core.mapper.JoinRow;
import org.jdbi.v3.core.mapper.JoinRowMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.StatementContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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

        // tag::userConstructor[]
        public User(int id, String name) {
            this.id = id;
            this.name = name;
        }
        // end::userConstructor[]
    }

    @Before
    public void setUp() {
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

    // tag::userMapper[]
    class UserMapper implements RowMapper<User> {
        @Override
        public User map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new User(rs.getInt("id"), rs.getString("name"));
        }
    }
    // end::userMapper[]

    @Test
    public void rowMapper() {
        // tag::rowMapper[]
        List<User> users = handle.createQuery("SELECT id, name FROM user ORDER BY id ASC")
            .map(new UserMapper())
            .list();
        // end::rowMapper[]

        assertThat(users).hasSize(4);
        assertThat(users.get(3).name).isEqualTo("Data");
    }

    @Test
    public void inlineRowMapper() {
        // tag::inlineRowMapper[]
        List<User> users = handle.createQuery("SELECT id, name FROM user ORDER BY id ASC")
                .map((rs, ctx) -> new User(rs.getInt("id"), rs.getString("name")))
                .list();
        // end::inlineRowMapper[]

        assertThat(users).hasSize(4);
        assertThat(users.get(3).name).isEqualTo("Data");
    }

    @Test
    public void rowMapperFactory() {
        // tag::rowMapperFactory[]
        handle.registerRowMapper(User.class, new UserMapper());

        handle.createQuery("SELECT id, name FROM user ORDER BY id ASC")
              .mapTo(User.class)
              .useStream(stream -> {
                  Optional<String> first = stream
                          .filter(u -> u.id > 2)
                          .map(u -> u.name)
                          .findFirst();
                  assertThat(first).contains("Charlie");
              });
        // end::rowMapperFactory[]
    }

    @Test
    public void constructorMapper() {
        // tag::constructorMapper[]
        handle.registerRowMapper(ConstructorMapper.factory(User.class));
        Set<User> userSet = handle.createQuery("SELECT * FROM user ORDER BY id ASC")
            .mapTo(User.class)
            .collect(Collectors.toSet());

        assertThat(userSet).hasSize(4);
        // end::constructorMapper[]
    }

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
        handle.registerRowMapper(ConstructorMapper.factory(NamedUser.class));

        NamedUser bob = handle.createQuery("SELECT id, name FROM user WHERE name = :name")
            .bind("name", "Bob")
            .mapTo(NamedUser.class)
            .findOnly();

        assertThat(bob.name.name).isEqualTo("Bob");
    }
    // end::columnMapper[]

    @Test
    public void beanMapper() {
        // tag::beanMapper[]
        handle.registerRowMapper(BeanMapper.factory(UserBean.class));

        List<UserBean> users = handle
                .createQuery("select id, name from user")
                .mapTo(UserBean.class)
                .list();
        // end::beanMapper[]

        assertThat(users).extracting("name").contains("Alice", "Bob", "Charlie", "Data");
    }

    @Test
    public void mapToBean() {
        // tag::mapToBean[]
        List<UserBean> users = handle
                .createQuery("select id, name from user")
                .mapToBean(UserBean.class)
                .list();
        // end::mapToBean[]

        assertThat(users).extracting("name").contains("Alice", "Bob", "Charlie", "Data");
    }

    @Test
    public void beanMapperPrefix() {
        handle.execute("create table contacts (id int, name text)");
        handle.execute("create table phones (id int, contact_id int, name text, number text)");

        handle.execute("insert into contacts (id, name) values (?, ?)", 1, "Alice");
        handle.execute("insert into phones (id, contact_id, name, number) values (?, ?, ?, ?)",
                100, 1, "Home", "555-1212");
        handle.execute("insert into phones (id, contact_id, name, number) values (?, ?, ?, ?)",
                101, 1, "Work", "555-9999");

        // tag::beanMapperPrefix[]
        handle.registerRowMapper(BeanMapper.factory(ContactBean.class, "c"));
        handle.registerRowMapper(BeanMapper.factory(PhoneBean.class, "p"));
        handle.registerRowMapper(JoinRowMapper.forTypes(ContactBean.class, PhoneBean.class));
        List<JoinRow> contactPhones = handle.select("select "
                + "c.id cid, c.name cname, "
                + "p.id pid, p.name pname, p.number pnumber "
                + "from contacts c left join phones p on c.id = p.contact_id")
                .mapTo(JoinRow.class)
                .list();
        // end::beanMapperPrefix[]

        assertThat(contactPhones)
                .extracting(cp -> cp.get(ContactBean.class), cp -> cp.get(PhoneBean.class))
                .containsExactly(
                        tuple(new ContactBean(1, "Alice"), new PhoneBean(100, "Home", "555-1212")),
                        tuple(new ContactBean(1, "Alice"), new PhoneBean(101, "Work", "555-9999")));
    }
}
