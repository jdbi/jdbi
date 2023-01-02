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
package org.jdbi.v3.vavr;

import java.util.Objects;
import java.util.Optional;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Multimap;
import io.vavr.collection.Seq;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.MapEntryMappers;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestVavrMapCollectorWithDB {

    private static final String VAL_PREFIX = "valCol";
    private static final String KEY_PREFIX = "keyCol";

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().installPlugins();

    private Seq<Integer> expected = List.range(0, 9);
    private Map<String, String> expectedMap = expected.toMap(i -> new Tuple2<>("keyCol" + i, "valCol" + (i + 1)));

    @BeforeEach
    public void addData() {
        h2Extension.getSharedHandle().execute("create table keyval ("
            + "idx int, "
            + "val_c varchar(10), "
            + "key_c varchar(10)"
            + ")");

        for (Integer i : expected) {
            h2Extension.getSharedHandle()
                .execute("insert into keyval(idx, val_c, key_c) values (?, ?, ?)", i, VAL_PREFIX + (i + 1), KEY_PREFIX + i);
        }
    }

    @Test
    public void testMapCollectorWithGlobalKeyValueShouldSucceed() {
        Jdbi jdbiWithKeyColAndValCol = h2Extension.getJdbi()
            .setMapKeyColumn("key_c")
            .setMapValueColumn("val_c");

        Boolean executed = jdbiWithKeyColAndValCol.withHandle(h -> {
            Map<String, String> valueMap = h.createQuery("select val_c, key_c from keyval")
                .collectInto(new GenericType<Map<String, String>>() {});
            assertThat(valueMap).containsOnlyElementsOf(expectedMap);
            return true;
        });

        assertThat(executed).isTrue();
    }

    @Test
    public void testMapCollectorWithTupleConfigShouldSucceed() {
        Map<String, String> valueMap = h2Extension.getSharedHandle()
            .configure(TupleMappers.class, c -> c.setKeyColumn("key_c").setValueColumn("val_c"))
            .createQuery("select val_c, key_c from keyval")
            .collectInto(new GenericType<Map<String, String>>() {});

        assertThat(valueMap).containsOnlyElementsOf(expectedMap);
    }

    @Test
    public void testMapCollectorWithCorrespondingTupleColsShouldSucceed() {
        Map<String, String> valueMap = h2Extension.getSharedHandle()
            .configure(TupleMappers.class, c -> c.setColumn(1, "key_c").setColumn(2, "val_c"))
            .createQuery("select val_c, key_c from keyval")
            .collectInto(new GenericType<Map<String, String>>() {});

        assertThat(valueMap).containsOnlyElementsOf(expectedMap);
    }

    @Test
    public void testSingleInstanceAssignmentWithSelectedKeyValueShouldSucceed() {
        Handle handle = h2Extension.getSharedHandle().configure(MapEntryMappers.class, c -> c.setKeyColumn("key_c").setValueColumn("val_c"));
        Optional<Tuple2<String, String>> valueMap = handle.createQuery("select val_c, key_c from keyval")
            .mapTo(new GenericType<Tuple2<String, String>>() {})
            .findFirst();

        assertThat(valueMap).isNotEmpty();
        assertThat(valueMap.get()).isEqualTo(Tuple.of(KEY_PREFIX + 0, VAL_PREFIX + 1));
    }

    /**
     * from {@link org.jdbi.v3.core.mapper.MapEntryMapperTest}
     */
    @Test
    public void uniqueIndex() {
        Handle h = h2Extension.getSharedHandle();
        h.execute("create table \"user\" (id int, name varchar)");
        h.prepareBatch("insert into \"user\" (id, name) values (?, ?)")
                .add(1, "alice")
                .add(2, "bob")
                .add(3, "cathy")
                .add(4, "dilbert")
                .execute();

        Map<Integer, User> map = h.createQuery("select * from \"user\"")
                .setMapKeyColumn("id")
                .registerRowMapper(ConstructorMapper.factory(User.class))
                .collectInto(new GenericType<Map<Integer, User>>() {});

        assertThat(map).containsOnly(
                Tuple.of(1, new User(1, "alice")),
                Tuple.of(2, new User(2, "bob")),
                Tuple.of(3, new User(3, "cathy")),
                Tuple.of(4, new User(4, "dilbert")));
    }

    @Test
    public void testNonUniqueIndexWithMultimap() {
        Handle h = h2Extension.getSharedHandle();
        h.execute("create table \"user\" (id int, name varchar)");
        h.prepareBatch("insert into \"user\" (id, name) values (?, ?)")
            .add(1, "alice")
            .add(2, "bob")
            .add(3, "alice")
            .execute();

        Multimap<String, User> usersByName = h.createQuery("select * from \"user\"")
            .setMapKeyColumn("name")
            .registerRowMapper(ConstructorMapper.factory(User.class))
            .collectInto(new GenericType<Multimap<String, User>>() {});

        assertThat(usersByName.apply("alice")).hasSize(2).containsExactly(
            new User(1, "alice"),
            new User(3, "alice"));
        assertThat(usersByName.apply("bob")).hasSize(1).containsExactly(
            new User(2, "bob"));
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
}
