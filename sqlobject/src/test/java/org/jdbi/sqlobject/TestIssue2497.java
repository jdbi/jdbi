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
package org.jdbi.sqlobject;

import java.lang.reflect.Type;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.argument.AbstractArgumentFactory;
import org.jdbi.core.argument.Argument;
import org.jdbi.core.argument.ArgumentFactory;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.generic.GenericType;
import org.jdbi.sqlobject.TestIssue2497.Parameters.Thing;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.customizer.BindBean;
import org.jdbi.sqlobject.statement.SqlBatch;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit5.JdbiExtension;
import org.jdbi.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestIssue2497 {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2()
        .withInitializer(TestingInitializers.something())
        .withPlugin(new SqlObjectPlugin());

    @Test
    public void testMapToArrayArgument() {
        final Jdbi db = h2Extension.getJdbi();
        try (Handle h = db.open()) {
            h.execute("create table foo (id identity primary key, v integer array)");
            h.registerArgument(new ListArrayArgumentFactory());
            h.execute("insert into foo (id, v) values (1, ?)", List.of(1, 2));
            h.execute("insert into foo (id, v) values (2, ?)", List.of(4, 3, 2));
            h.execute("insert into foo (id, v) values (3, ?)", List.of(5, 6));

            List<Integer> result = h.createQuery("SELECT v FROM foo WHERE id = :id")
                .bind("id", 2)
                .mapTo(new GenericType<List<Integer>>() {})
                .one();

            assertThat(result).isNotNull().hasSize(3).containsExactly(4, 3, 2);
        }
    }

    public static class ListArrayArgumentFactory extends AbstractArgumentFactory<List<Integer>> {

        public ListArrayArgumentFactory() {
            super(Types.INTEGER);
        }

        @Override
        protected Argument build(List<Integer> value, ConfigRegistry config) {
            return (position, statement, ctx) -> statement.setObject(position, value.toArray(new Integer[0]));
        }
    }

    @Test
    public void testMapToStringArgument() {
        final Jdbi db = h2Extension.getJdbi();
        try (Handle h = db.open()) {
            h.registerArgument(new ListStringFactory());
            h.execute("insert into something (id, name) values (1, ?)", List.of("Hello", "World"));
            h.execute("insert into something (id, name) values (2, ?)", List.of("The", "quick", "brown", "fox"));
            h.execute("insert into something (id, name) values (3, ?)", List.of("Now", "is", "the", "time"));
            h.execute("insert into something (id, name) values (4, ?)", (List<String>) null);

            String result = h.createQuery("SELECT name FROM something WHERE id = :id")
                .bind("id", 2)
                .mapTo(String.class)
                .one();

            assertThat(result).isEqualTo("[\"The\",\"quick\",\"brown\",\"fox\"]");

            int count = h.createQuery("SELECT count(1) FROM something")
                .mapTo(Integer.class)
                .one();

            assertThat(count).isEqualTo(4);
        }
    }

    public static class ListStringFactory extends AbstractArgumentFactory<List<String>> {
        public ListStringFactory() {
            super(Types.VARCHAR);
        }

        @Override
        protected Argument build(List<String> value, ConfigRegistry config) {
            return (position, statement, ctx) -> {
                if (value == null) {
                    statement.setNull(position, Types.NULL);
                } else {
                    statement.setString(position, "[" + value.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",")) + "]");
                }
            };
        }
    }

    @Test
    public void testMapToObjectArgument() {
        final Jdbi db = h2Extension.getJdbi();
        db.registerArgument(new ListObjectFactory());

        try (Handle h = db.open()) {
            h.execute("insert into something (id, name) values (1, ?)",
                List.of(Parameters.thing("Hello"), Parameters.thing("World")));
            h.execute("insert into something (id, name) values (2, ?)",
                List.of(Parameters.thing("The"), Parameters.thing("quick"), Parameters.thing("brown"), Parameters.thing("fox")));
            h.execute("insert into something (id, name) values (3, ?)",
                List.of(Parameters.thing("Now"), Parameters.thing("is"), Parameters.thing("the"), Parameters.thing("time")));
            h.execute("insert into something (id, name) values (4, ?)", (List<Thing>) null);

            String result = h.createQuery("SELECT name FROM something WHERE id = :id")
                .bind("id", 2)
                .mapTo(String.class)
                .one();

            assertThat(result).isEqualTo("[\"The\",\"quick\",\"brown\",\"fox\"]");

            int count = h.createQuery("SELECT count(1) FROM something")
                .mapTo(Integer.class)
                .one();

            assertThat(count).isEqualTo(4);
        }
    }

    @Test
    public void testNestedSqlObject() {
        final Jdbi db = h2Extension.getJdbi();

        List<Sulu> sulus = Arrays.asList(new Sulu(1, "George", "Takei"),
            new Sulu(2, "John", "Cho"));
        db.useExtension(SuluDao.class, s -> {
            Handle h = s.getHandle();
            h.registerArgument(new SuluArgumentFactory());
            SuluDao dao = h.attach(SuluDao.class);
            dao.insertSulus(sulus);
        });

        db.useExtension(SuluDao.class, s -> {
            assertThat(s.findName(1)).isEqualTo("George Takei");
            assertThat(s.findName(2)).isEqualTo("John Cho");
        });
    }

    @Test
    public void testMixFluentAndSqlObject() {
        final Jdbi db = h2Extension.getJdbi();

        List<Sulu> sulus = Arrays.asList(
            new Sulu(1, "George", "Takei"),
            new Sulu(2, "John", "Cho"));

        db.withHandle(h -> {
            h.registerArgument(new SuluArgumentFactory());
            SuluDao dao = h.attach(SuluDao.class);
            dao.insertSulus(sulus);
            return null;
        });

        db.useExtension(SuluDao.class, s -> {
            assertThat(s.findName(1)).isEqualTo("George Takei");
            assertThat(s.findName(2)).isEqualTo("John Cho");
        });
    }

    public static class Parameters {
        public static Thing thing(String value) {
            return new Thing(value);
        }

        public static class Thing {
            private final String value;

            private Thing(String value) {
                this.value = value;
            }

            public String getValue() {
                return value;
            }
        }
    }

    public static class ListObjectFactory extends AbstractArgumentFactory<List<Thing>> {
        public ListObjectFactory() {
            super(Types.VARCHAR);
        }

        @Override
        protected Argument build(List<Thing> value, ConfigRegistry config) {
            return (position, statement, ctx) -> {
                if (value == null) {
                    statement.setNull(position, Types.NULL);
                } else {
                    statement.setString(position,
                        "[" + value.stream()
                                    .map(Thing::getValue)
                                    .map(s -> "\"" + s + "\"")
                                    .collect(Collectors.joining(","))
                            + "]");
                }
            };
        }
    }

    public interface SuluDao extends SqlObject {
        @SqlBatch("insert into something (id, name) values (:sulu.id, :bean)")
        void insertSulus(@Bind("bean") @BindBean("sulu") List<Sulu> sulus);

        @SqlQuery("select name from something where id = :id")
        String findName(@Bind("id") int id);
    }

    public static class Sulu {
        private final int id;
        private final String firstName;
        private final String lastName;

        public Sulu(int id, String firstName, String lastName) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return firstName + " " + lastName;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Sulu.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("firstName='" + firstName + "'")
                .add("lastName='" + lastName + "'")
                .toString();
        }
    }

    public static final class SuluArgumentFactory implements ArgumentFactory {

        @Override
        public Optional<Argument> build(Type type, Object value, ConfigRegistry config) {
            return Optional.of(type)
                .filter(t -> t == Sulu.class)
                .map(t -> (position, statement, ctx) -> {
                    Sulu sulu = (Sulu) value;
                    statement.setString(position, sulu.getName());
                });
        }
    }
}
