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

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.Map;
import java.util.Objects;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.guava.api.Assertions.assertThat;

public class TestReturningMap {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugins();

    private Handle h;

    @Before
    public void setUp() {
        h = dbRule.getSharedHandle();
    }

    @Test
    public void keyValueColumns() {
        KeyValueDao dao = h.attach(KeyValueDao.class);

        dao.createTable();
        dao.insert("foo", "123");
        dao.insert("bar", "xyz");

        assertThat(dao.getAll()).containsOnly(
                entry("foo", "123"),
                entry("bar", "xyz"));
    }

    public interface KeyValueDao {
        @SqlUpdate("create table config (key varchar, value varchar)")
        void createTable();

        @SqlUpdate("insert into config (key, value) values (:key, :value)")
        void insert(String key, String value);

        // tag::keyValue[]
        @SqlQuery("select key, value from config")
        @KeyColumn("key")
        @ValueColumn("value")
        Map<String, String> getAll();
        // end::keyValue[]
    }

    @Test
    public void uniqueIndex() {
        UniqueIndexDao dao = h.attach(UniqueIndexDao.class);

        dao.createTable();

        dao.insert(
                new User(1, "alice"),
                new User(2, "bob"),
                new User(3, "cathy"),
                new User(4, "dilbert"));

        assertThat(dao.getAll()).containsOnly(
                entry(1, new User(1, "alice")),
                entry(2, new User(2, "bob")),
                entry(3, new User(3, "cathy")),
                entry(4, new User(4, "dilbert")));
    }

    public interface UniqueIndexDao {
        @SqlUpdate("create table user (id int, name varchar)")
        void createTable();

        @SqlBatch("insert into user (id, name) values (:id, :name)")
        void insert(@BindBean User... users);

        // tag::uniqueIndex[]
        @SqlQuery("select * from user")
        @KeyColumn("id")
        @RegisterConstructorMapper(User.class)
        Map<Integer, User> getAll();
        // end::uniqueIndex[]
    }

    @Test
    public void joinRow() {
        JoinRowDao dao = h.attach(JoinRowDao.class);
        dao.createUserTable();
        dao.createPhoneTable();

        dao.insertUsers(
                new User(1, "alice"),
                new User(2, "bob"),
                new User(3, "cathy"));
        dao.insertPhone(1, new Phone(10, "555-0001"));
        dao.insertPhone(2, new Phone(20, "555-0002"));
        dao.insertPhone(3, new Phone(30, "555-0003"));

        assertThat(dao.getMap()).containsOnly(
                entry(new User(1, "alice"), new Phone(10, "555-0001")),
                entry(new User(2, "bob"), new Phone(20, "555-0002")),
                entry(new User(3, "cathy"), new Phone(30, "555-0003")));
    }

    @Test
    public void multimapJoin() {
        JoinRowDao dao = h.attach(JoinRowDao.class);
        dao.createUserTable();
        dao.createPhoneTable();

        dao.insertUsers(
                new User(1, "alice"),
                new User(2, "bob"),
                new User(3, "cathy"));
        dao.insertPhone(1, new Phone(10, "555-0001"), new Phone(11, "555-0021"));
        dao.insertPhone(2, new Phone(20, "555-0002"), new Phone(21, "555-0022"));
        dao.insertPhone(3, new Phone(30, "555-0003"), new Phone(31, "555-0023"));

        assertThat(dao.getMultimap()).hasSameEntriesAs(
                ImmutableMultimap.<User, Phone>builder()
                        .putAll(new User(1, "alice"), new Phone(10, "555-0001"), new Phone(11, "555-0021"))
                        .putAll(new User(2, "bob"),   new Phone(20, "555-0002"), new Phone(21, "555-0022"))
                        .putAll(new User(3, "cathy"), new Phone(30, "555-0003"), new Phone(31, "555-0023"))
                        .build());
    }

    public interface JoinRowDao {
        @SqlUpdate("create table user (id int, name varchar)")
        void createUserTable();

        @SqlUpdate("create table phone (id int, user_id int, phone varchar)")
        void createPhoneTable();

        @SqlBatch("insert into user (id, name) values (:id, :name)")
        void insertUsers(@BindBean User... users);

        @SqlBatch("insert into phone (id, user_id, phone) values (:id, :userId, :phone)")
        void insertPhone(int userId, @BindBean Phone... phones);

        // tag::joinRow[]
        @SqlQuery("select u.id u_id, u.name u_name, p.id p_id, p.phone p_phone "
            + "from user u left join phone p on u.id = p.user_id")
        @RegisterConstructorMapper(value = User.class, prefix = "u")
        @RegisterConstructorMapper(value = Phone.class, prefix = "p")
        Map<User, Phone> getMap();
        // end::joinRow[]

        // tag::joinRowMultimap[]
        @SqlQuery("select u.id u_id, u.name u_name, p.id p_id, p.phone p_phone "
            + "from user u left join phone p on u.id = p.user_id")
        @RegisterConstructorMapper(value = User.class, prefix = "u")
        @RegisterConstructorMapper(value = Phone.class, prefix = "p")
        Multimap<User, Phone> getMultimap();
        // end::joinRowMultimap[]

    }

    public static class User {
        private final int id;
        private final String name;

        public User(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
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
            return id == user.id
                && Objects.equals(name, user.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }

        @Override
        public String toString() {
            return "User{"
                + "id=" + id
                + ", name='" + name + '\''
                + '}';
        }
    }

    public static class Phone {
        private final int id;
        private final String phone;

        public Phone(int id, String phone) {
            this.id = id;
            this.phone = phone;
        }

        public int getId() {
            return id;
        }

        public String getPhone() {
            return phone;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Phone phone1 = (Phone) o;
            return id == phone1.id
                && Objects.equals(phone, phone1.phone);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, phone);
        }

        @Override
        public String toString() {
            return "Phone{"
                + "id=" + id
                + ", phone='" + phone + '\''
                + '}';
        }
    }
}
