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

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Optional;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestGuavaOptional {

    private static final String SELECT_BY_NAME = "select * from something "
        + "where :name is null or name = :name "
        + "order by id";

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().installPlugins().withInitializer(TestingInitializers.something());

    Handle handle;

    @BeforeEach
    public void createTestData() {
        handle = h2Extension.openHandle();
        handle.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        handle.createUpdate("insert into something (id, name) values (2, 'brian')").execute();
    }

    @AfterEach
    public void tearDown() {
        handle.close();
    }

    @Test
    public void testDynamicBindOptionalPresent() {
        Something result = handle.createQuery(SELECT_BY_NAME)
            .bindByType("name", Optional.of("eric"), new GenericType<Optional<String>>() {})
            .mapToBean(Something.class)
            .one();
        assertThat(result).isEqualTo(new Something(1, "eric"));
    }

    @Test
    public void testDynamicBindOptionalEmpty() {
        List<Something> result = handle.createQuery(SELECT_BY_NAME)
            .bindByType("name", Optional.absent(), new GenericType<Optional<String>>() {})
            .mapToBean(Something.class)
            .list();

        assertThat(result).containsExactly(new Something(1, "eric"), new Something(2, "brian"));
    }

    @Test
    public void testDynamicBindOptionalOfCustomType() {
        handle.registerArgument(new NameArgumentFactory());
        List<Something> result = handle.createQuery(SELECT_BY_NAME)
            .bindByType("name", Optional.of(new Name("eric")), new GenericType<Optional<Name>>() {})
            .mapToBean(Something.class)
            .list();

        assertThat(result).hasSize(1).containsExactly(new Something(1, "eric"));
    }

    @Test
    public void testDynamicBindOptionalOfUnregisteredCustomType() {
        try (Query query = handle.createQuery(SELECT_BY_NAME)) {
            ResultIterable<Something> ri = query
                .bindByType("name", Optional.of(new Name("eric")), new GenericType<Optional<Name>>() {})
                .mapToBean(Something.class);
            assertThatThrownBy(ri::list).isInstanceOf(UnableToCreateStatementException.class);
        }
    }

    @Test
    public void testBindOptionalPresent() {
        Something result = handle.createQuery(SELECT_BY_NAME)
            .bind("name", Optional.of("brian"))
            .mapToBean(Something.class)
            .one();

        assertThat(result).isEqualTo(new Something(2, "brian"));
    }

    @Test
    public void testBindOptionalEmpty() {
        List<Something> result = handle.createQuery(SELECT_BY_NAME)
            .bind("name", Optional.absent())
            .mapToBean(Something.class)
            .list();

        assertThat(result).containsExactly(new Something(1, "eric"), new Something(2, "brian"));
    }

    @Test
    public void testBindOptionalOfCustomType() {
        handle.registerArgument(new NameArgumentFactory());
        List<Something> result = handle.createQuery(SELECT_BY_NAME)
            .bind("name", Optional.of(new Name("eric")))
            .mapToBean(Something.class)
            .list();

        assertThat(result).containsExactly(new Something(1, "eric"));
    }

    @Test
    public void testBindOptionalOfUnregisteredCustomType() {
        try (Query query = handle.createQuery(SELECT_BY_NAME)) {
            ResultIterable<Something> ri = query
                .bind("name", Optional.of(new Name("eric")))
                .mapToBean(Something.class);
            assertThatThrownBy(ri::list).isInstanceOf(UnableToCreateStatementException.class);
        }
    }

    class Name {
        final String value;

        Name(String value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Name)) {
                return false;
            }
            Name that = (Name) obj;
            return this.value.equals(that.value);
        }
    }

    class NameArgumentFactory implements ArgumentFactory {
        @Override
        public java.util.Optional<Argument> build(Type expectedType, Object value, ConfigRegistry config) {
            if (expectedType == Name.class) {
                Name nameValue = (Name) value;
                return java.util.Optional.of((pos, stmt, c) -> stmt.setString(pos, nameValue.value));
            }
            return java.util.Optional.empty();
        }
    }

}
