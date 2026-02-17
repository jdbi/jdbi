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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.jdbi.core.generic.GenericType;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.statement.StatementContext;
import org.jdbi.guava.GuavaPlugin;
import org.jdbi.sqlobject.config.KeyColumn;
import org.jdbi.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.guava.api.Assertions.assertThat;

public class TestIssue2623 {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2()
        .withInitializer(TestingInitializers.usersWithData())
        .withPlugins(new SqlObjectPlugin(), new GuavaPlugin());

    @BeforeEach
    public void setUp() {
        h2Extension.getJdbi().registerRowMapper(new GenericType<>() {}, new UserIntegerMapper());
    }

    @Test
    public void testParameterizeTypedMapper() {
        var jdbi = h2Extension.getJdbi();

        Multimap<Integer, User<Integer>> result = jdbi.withExtension(Dao.class, dao -> dao.getUsers(1));
        assertThat(result).isNotNull().containsAllEntriesOf(ImmutableMultimap.of(1, new User<>("Alice", 1)));
    }

    @Test
    public void testConcreteTypedMapper() {
        var jdbi = h2Extension.getJdbi();

        Multimap<Integer, IntUser> result = jdbi.withExtension(Dao.class, dao -> dao.getIntUsers(1));
        assertThat(result).isNotNull().containsAllEntriesOf(ImmutableMultimap.of(1, new IntUser("Alice", 1)));
    }

    public interface Dao {
        @SqlQuery("select name, id from users where id = :id")
        @KeyColumn("id")
        Multimap<Integer, User<Integer>> getUsers(int id);

        @SqlQuery("select name, id from users where id = :id")
        @KeyColumn("id")
        @RegisterConstructorMapper(IntUser.class)
        Multimap<Integer, IntUser> getIntUsers(int id);
    }

    public record User<T>(String name, T id) {}

    public static class UserIntegerMapper implements RowMapper<User<Integer>> {
        @Override
        public User<Integer> map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new User<>(rs.getString("name"), rs.getInt("id"));
        }
    }

    public static class IntUser {
        private final String name;
        private final Integer id;

        public IntUser(String name, Integer id) {
            this.name = name;
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public Integer getId() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            IntUser intUser = (IntUser) o;
            return Objects.equals(name, intUser.name) && Objects.equals(id, intUser.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, id);
        }
    }
}
