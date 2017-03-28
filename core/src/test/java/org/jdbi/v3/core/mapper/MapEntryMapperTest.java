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
package org.jdbi.v3.core.mapper;

import java.util.Map;
import java.util.Objects;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class MapEntryMapperTest {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

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
                .add("bar", "xyz")
                .execute();

        Map<String, String> map = h.createQuery("select key, value from config")
                .configure(MapEntryMapper.Config.class, cfg -> cfg.setKeyColumn("key").setValueColumn("value"))
                .collectInto(new GenericType<Map<String, String>>() {});

        assertThat(map).containsOnly(
                entry("foo", "123"),
                entry("bar", "xyz"));
    }

    @Test
    public void uniqueIndex() {
        h.execute("create table user (id int, name varchar)");
        h.prepareBatch("insert into user (id, name) values (?, ?)")
                .add(1, "alice")
                .add(2, "bob")
                .add(3, "cathy")
                .add(4, "dilbert")
                .execute();

        Map<Integer, User> map = h.createQuery("select * from user")
                .configure(MapEntryMapper.Config.class, cfg -> cfg.setKeyColumn("id"))
                .registerRowMapper(ConstructorMapper.factory(User.class))
                .collectInto(new GenericType<Map<Integer, User>>() {});

        assertThat(map).containsOnly(
                entry(1, new User(1, "alice")),
                entry(2, new User(2, "bob")),
                entry(3, new User(3, "cathy")),
                entry(4, new User(4, "dilbert")));
    }

    @Test
    public void joinRow() {
        h.execute("create table user (id int, name varchar)");
        h.execute("create table phone (id int, user_id int, phone varchar)");
        h.prepareBatch("insert into user (id, name) values (?, ?)")
                .add(1, "alice")
                .add(2, "bob")
                .add(3, "cathy")
                .execute();
        h.prepareBatch("insert into phone (id, user_id, phone) values (?, ?, ?)")
                .add(10, 1, "555-0001")
                .add(20, 2, "555-0002")
                .add(30, 3, "555-0003")
                .execute();

        String sql = "select u.id u_id, u.name u_name, p.id p_id, p.phone p_phone " +
                "from user u left join phone p on u.id = p.user_id";
        Map<User, Phone> map = h.createQuery(sql)
                .registerRowMapper(ConstructorMapper.factory(User.class, "u"))
                .registerRowMapper(ConstructorMapper.factory(Phone.class, "p"))
                .collectInto(new GenericType<Map<User, Phone>>() {});

        assertThat(map).containsOnly(
                entry(new User(1, "alice"), new Phone(10, "555-0001")),
                entry(new User(2, "bob"),   new Phone(20, "555-0002")),
                entry(new User(3, "cathy"), new Phone(30, "555-0003")));
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
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            User user = (User) o;
            return id == user.id &&
                    Objects.equals(name, user.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }

        @Override
        public String toString() {
            return "User{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    '}';
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
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Phone phone1 = (Phone) o;
            return id == phone1.id &&
                    Objects.equals(phone, phone1.phone);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, phone);
        }

        @Override
        public String toString() {
            return "Phone{" +
                    "id=" + id +
                    ", phone='" + phone + '\'' +
                    '}';
        }
    }
}
