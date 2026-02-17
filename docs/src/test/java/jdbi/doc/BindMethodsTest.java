package jdbi.doc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jdbi.core.Handle;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.statement.StatementContext;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.config.RegisterBeanMapper;
import org.jdbi.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.sqlobject.config.RegisterFieldMapper;
import org.jdbi.sqlobject.customizer.BindBean;
import org.jdbi.sqlobject.customizer.BindFields;
import org.jdbi.sqlobject.customizer.BindMethods;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class BindMethodsTest {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin()).withInitializer(TestingInitializers.users());

    UserDao dao;

    @BeforeEach
    void setUp() {
        Handle handle = h2Extension.getSharedHandle();
        handle.registerRowMapper(new UserMapper());

        this.dao = handle.attach(UserDao.class);

    }

    @Test
    void testUserList() {
        User user = new User(2, "two");
        dao.insertUser(user);

        List<User> result = dao.getUsers();
        assertThat(result).hasSize(1).contains(user);
    }

    @Test
    void testUser() {
        User user = new User(2, "two");
        dao.insertUser(user);

        Optional<User> result = dao.getUser(2);
        assertThat(result).isPresent().contains(user);
    }

    @Test
    void testUserField() {
        UserField userField = new UserField();
        userField.id = 2;
        userField.name = "two";
        dao.insertUserField(userField);

        Optional<UserField> result = dao.getUserField(2);
        assertThat(result).isPresent().contains(userField);
    }

    @Test
    void testUserFieldList() {
        UserField userField = new UserField();
        userField.id = 2;
        userField.name = "two";
        dao.insertUserField(userField);

        List<User> result = dao.getUsers();
        assertThat(result).hasSize(1).contains(new User(2, "two"));
    }


    @Test
    void testUserBeanList() {
        UserBean userBean = new UserBean();
        userBean.setId(2);
        userBean.setName("two");
        dao.insertUserBean(userBean);

        List<User> result = dao.getUsers();
        assertThat(result).hasSize(1).contains(new User(2, "two"));
    }

    @Test
    void testUserBean() {
        UserBean userBean = new UserBean();
        userBean.setId(2);
        userBean.setName("two");
        dao.insertUserBean(userBean);

        Optional<UserBean> result = dao.getUserBean(2);
        assertThat(result).isPresent().contains(userBean);
    }

    @Test
    void testUserBeanPrefix() {
        UserBean userBean = new UserBean();
        userBean.setId(2);
        userBean.setName("two");
        dao.insertUserBeanPrefix(userBean);

        Optional<UserBean> result = dao.getUserBeanPrefix(2);
        assertThat(result).isPresent().contains(userBean);
    }

    @Test
    void testNestedUser() {
        User user = new User(2, "two");
        NestedUser nested = new NestedUser(user, "tagged");
        dao.insertUser(nested);

        Optional<User> result = dao.getUser(2);
        assertThat(result).isPresent().contains(user);
    }

    @Test
    void testConsumeUser() {
        User user = new User(2, "two");
        dao.insertUser(user);

        dao.consumeUser(2, result -> assertThat(result).isEqualTo(user));
    }

    @Test
    void testConsumeMultiUser() {
        User user1 = new User(1, "one");
        User user2 = new User(2, "two");
        User user3 = new User(3, "three");
        dao.insertUser(user1);
        dao.insertUser(user2);
        dao.insertUser(user3);

        List<User> users = Arrays.asList(user1, user2, user3);

        dao.consumeMultiUsers(result -> assertThat(result).isIn(users));
    }

    @Test
    void testConsumeMultiUserIterable() {
        User user1 = new User(1, "one");
        User user2 = new User(2, "two");
        User user3 = new User(3, "three");
        dao.insertUser(user1);
        dao.insertUser(user2);
        dao.insertUser(user3);

        dao.consumeMultiUserIterable(result -> assertThat(result).containsExactlyInAnyOrder(user1, user2, user3));
    }


    public interface UserDao {

        @SqlQuery("SELECT * FROM users")
        List<User> getUsers();

        // tag::user-method[]
        @SqlUpdate("INSERT INTO users (id, name) VALUES (:id, :name)")
        void insertUser(@BindMethods User user);
        // end::user-method[]

        // tag::user-bean-method[]
        @SqlUpdate("INSERT INTO users (id, name) VALUES (:id, :name)")
        void insertUserBean(@BindBean UserBean user);
        // end::user-bean-method[]

        // tag::user-field-method[]
        @SqlUpdate("INSERT INTO users (id, name) VALUES (:id, :name)")
        void insertUserField(@BindFields UserField user);
        // end::user-field-method[]

        // tag::user-bean-prefix-method[]
        @SqlUpdate("INSERT INTO users (id, name) VALUES (:user.id, :user.name)")
        // <1>
        void insertUserBeanPrefix(@BindBean("user") UserBean user); // <2>
        // end::user-bean-prefix-method[]

        // tag::nested-user-method[]
        @SqlUpdate("INSERT INTO users (id, name) VALUES (:user.id, :user.name)")
        void insertUser(@BindMethods NestedUser user);
        // end::nested-user-method[]

        @SqlQuery("SELECT * FROM users where id = :id")
        @RegisterConstructorMapper(User.class)
        Optional<User> getUser(int id);

        @SqlQuery("SELECT * FROM users where id = :id")
        @RegisterBeanMapper(UserBean.class)
        Optional<UserBean> getUserBean(int id);

        @SqlQuery("SELECT name as user_name, id as user_id FROM users where id = :id")
        @RegisterBeanMapper(value = UserBean.class, prefix = "user")
        Optional<UserBean> getUserBeanPrefix(int id);

        @SqlQuery("SELECT * FROM users where id = :id")
        @RegisterFieldMapper(UserField.class)
        Optional<UserField> getUserField(int id);

        // tag::consume-user-method[]
        @SqlQuery("SELECT * FROM users WHERE id = :id")
        @RegisterConstructorMapper(User.class)
        void consumeUser(int id, Consumer<User> consumer); // <1>

        @SqlQuery("SELECT * FROM users")
        @RegisterConstructorMapper(User.class)
        void consumeMultiUsers(Consumer<User> consumer); // <2>
        // end::consume-user-method[]

        // tag::consume-iterable-user-method[]
        @SqlQuery("SELECT * FROM users")
        @RegisterConstructorMapper(User.class)
        void consumeMultiUserIterable(Consumer<Iterable<User>> consumer); // <1>
        // end::consume-iterable-user-method[]

        // tag::function-user-method[]
        @SqlQuery("SELECT * FROM users")
        @RegisterConstructorMapper(User.class)
        Set<User> mapUsers(Function<Stream<User>, Set<User>> function); // <1>
        // end::function-user-method[]
    }


    public static
            // tag::user[]
        class User {

        private final int id;
        private final String name;

        public User(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int id() {
            return id;
        }

        public String name() {
            return name;
        }
        // end::user[]

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            User user = (User) o;
            return id == user.id && Objects.equals(name, user.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
    }

    public static
            // tag::user-bean[]
        class UserBean {

        private int id;
        private String name;

        public UserBean() {}

        public void setId(int id) {
            this.id = id;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
        // end::user-bean[]

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            UserBean that = (UserBean) o;
            return id == that.id && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
    }

    public static
            // tag::user-field[]
        class UserField {

        public int id;
        public String name;
        // end::user-field[]


        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            UserField userField = (UserField) o;
            return id == userField.id && Objects.equals(name, userField.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
    }

    public static
            // tag::nested-user[]
        class NestedUser {

        private final User user;
        private final String tag;

        public NestedUser(User user, String tag) {
            this.user = user;
            this.tag = tag;
        }

        public User user() {
            return user;
        }

        public String tag() {
            return tag;
        }
    }
    // end::nested-user[]


    static class UserMapper implements RowMapper<User> {

        @Override
        public User map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new User(rs.getInt("id"), rs.getString("name"));
        }
    }
}
