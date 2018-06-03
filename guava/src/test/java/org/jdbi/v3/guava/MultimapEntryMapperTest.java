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
package org.jdbi.v3.guava;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import java.util.Objects;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.guava.api.Assertions.assertThat;

public class MultimapEntryMapperTest {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new GuavaPlugin());

    private Handle h;

    @Before
    public void setUp() {
        h = dbRule.getSharedHandle();
    }

    @Test
    public void keyValueColumns() {
        h.execute("create table config (key varchar, value varchar)");
        h.prepareBatch("insert into config (key, value) values (?, ?)")
                .add("foo", "123")
                .add("foo", "456")
                .add("bar", "xyz")
                .execute();

        // tag::keyValue[]
        Multimap<String, String> map = h.createQuery("select key, value from config")
                .setMapKeyColumn("key")
                .setMapValueColumn("value")
                .collectInto(new GenericType<Multimap<String, String>>() {});
        // end::keyValue[]

        Multimap<String, String> expected = ImmutableListMultimap.<String, String>builder()
                .putAll("foo", "123", "456")
                .put("bar", "xyz")
                .build();
        assertThat(map).isEqualTo(expected);
    }

    @Test
    public void index() {
        h.execute("create table user (id int, manager_id int, name varchar)");
        h.prepareBatch("insert into user (id, manager_id, name) values (?, ?, ?)")
                .add(1, 0, "alice")
                .add(2, 1, "bob")
                .add(3, 1, "cathy")
                .add(4, 3, "dilbert")
                .execute();

        // tag::index[]
        Multimap<Integer, User> map = h.createQuery("select id, manager_id, name from user")
                .setMapKeyColumn("manager_id")
                .registerRowMapper(ConstructorMapper.factory(User.class))
                .collectInto(new GenericType<Multimap<Integer, User>>() {});
        // end::index[]

        Multimap<Integer, User> expected = ImmutableListMultimap.<Integer, User>builder()
                .put(0, new User(1, "alice"))
                .putAll(1, new User(2, "bob"), new User(3, "cathy"))
                .put(3, new User(4, "dilbert"))
                .build();
        assertThat(map).isEqualTo(expected);
    }

    @Test
    public void joinRow() {
        h.execute("create table user (id int, name varchar)");
        h.execute("create table phone (id int, user_id int, phone varchar)");
        h.prepareBatch("insert into user (id, name) values (?, ?)")
                .add(1, "alice")
                .add(2, "bob")
                .execute();
        h.prepareBatch("insert into phone (id, user_id, phone) values (?, ?, ?)")
                .add(10, 1, "555-0001")
                .add(11, 1, "555-0002")
                .add(20, 2, "555-0003")
                .execute();

        // tag::joinRow[]
        String sql = "select u.id u_id, u.name u_name, p.id p_id, p.phone p_phone "
            + "from user u left join phone p on u.id = p.user_id";
        Multimap<User, Phone> map = h.createQuery(sql)
                .registerRowMapper(ConstructorMapper.factory(User.class, "u"))
                .registerRowMapper(ConstructorMapper.factory(Phone.class, "p"))
                .collectInto(new GenericType<Multimap<User, Phone>>() {});
        // end::joinRow[]

        Multimap<User, Phone> expected = ImmutableListMultimap.<User, Phone>builder()
                .putAll(new User(1, "alice"), new Phone(10, "555-0001"), new Phone(11, "555-0002"))
                .put(new User(2, "bob"), new Phone(20, "555-0003"))
                .build();
        assertThat(map).isEqualTo(expected);
    }

    public static class User {
        private final int id;
        private final String name;

        public User(int id, String name) {
            this.id = id;
            this.name = name;
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
