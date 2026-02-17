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
import java.util.Optional;

import org.jdbi.core.Jdbi;
import org.jdbi.core.argument.Argument;
import org.jdbi.core.argument.ArgumentFactory;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.sqlobject.config.RegisterArgumentFactory;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit5.JdbiExtension;
import org.jdbi.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRegisterArgumentFactory {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());
    private Jdbi db;

    @BeforeEach
    public void setUp() {
        db = h2Extension.getJdbi();
    }

    @Test
    public void testSingleAnnotation() {
        db.useExtension(Waffle.class, w -> {
            w.insert(1, new Name("Brian", "McCallister"));

            assertThat(w.findName(1)).isEqualTo("Brian McCallister");
        });
    }

    @Test
    public void testMultipleAnnotations() {
        db.useExtension(ShortStack.class, s -> {
            s.insert(1, new Name("George", "Takei"));

            assertThat(s.findName(1)).isEqualTo("George Takei");
        });
    }

    @RegisterArgumentFactory(NameAF.class)
    public interface Waffle {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") Name name);

        @SqlQuery("select name from something where id = :id")
        String findName(@Bind("id") int id);
    }

    @RegisterArgumentFactory(NameAF.class)
    @RegisterArgumentFactory(LazyAF.class)
    public interface ShortStack {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") Name name);

        @SqlQuery("select name from something where id = :id")
        String findName(@Bind("id") int id);
    }

    public static class LazyAF implements ArgumentFactory {
        @Override
        public Optional<Argument> build(Type type, Object value, ConfigRegistry config) {
            return Optional.empty();
        }
    }

    public static class NameAF implements ArgumentFactory {
        @Override
        public Optional<Argument> build(Type expectedType, Object value, ConfigRegistry config) {
            if (expectedType == Name.class || value instanceof Name) {
                Name nameValue = (Name) value;
                return Optional.of((position, statement, ctx1) -> statement.setString(position, nameValue.getFullName()));
            }
            return Optional.empty();
        }
    }

    public static class Name {
        private final String first;
        private final String last;

        public Name(String first, String last) {

            this.first = first;
            this.last = last;
        }

        public String getFullName() {
            return first + " " + last;
        }

        @Override
        public String toString() {
            return "<Name first=" + first + " last=" + last + " >";
        }
    }

}
