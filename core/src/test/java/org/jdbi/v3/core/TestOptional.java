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
package org.jdbi.v3.core;

import java.util.Objects;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestOptional {
    private static final String SELECT_BY_NAME = "select * from something "
        + "where :name is null or name = :name "
        + "order by id";

    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    Handle handle;

    @Before
    public void createTestData() {
        handle = dbRule.openHandle();
        handle.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        handle.createUpdate("insert into something (id, name) values (2, 'brian')").execute();
    }

    @Test
    public void testMapToOptional() throws Exception {
        GenericType<Optional<String>> optionalString = new GenericType<Optional<String>>() {};

        assertThat(handle.select("select name from something where id = 0")
            .collectInto(optionalString))
            .isEmpty();

        assertThat(handle.select("select null from something where id = 1")
            .collectInto(optionalString))
            .isEmpty();

        assertThat(handle.select("select name from something where id = 1")
            .collectInto(optionalString))
            .hasValue("eric");

        assertThatThrownBy(() -> handle.select("select name from something")
            .collectInto(optionalString))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Multiple values for optional");
    }

    @Test
    public void testMapToOptionalInt() throws Exception {
        assertThat(handle.select("select id from something where name = 'arthur'")
            .collectInto(OptionalInt.class))
            .isEmpty();

        assertThat(handle.select("select null from something where name = 'eric'")
            .collectInto(OptionalInt.class))
            .isEmpty();

        assertThat(handle.select("select id from something where name = 'eric'")
            .collectInto(OptionalInt.class))
            .hasValue(1);

        assertThatThrownBy(() -> handle.select("select id from something")
            .collectInto(OptionalInt.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Multiple values for optional");
    }

    @Test
    public void testMapToOptionalLong() throws Exception {
        assertThat(handle.select("select id from something where name = 'ford'")
            .collectInto(OptionalLong.class))
            .isEmpty();

        assertThat(handle.select("select null from something where name = 'eric'")
            .collectInto(OptionalLong.class))
            .isEmpty();

        assertThat(handle.select("select id from something where name = 'eric'")
            .collectInto(OptionalLong.class))
            .hasValue(1L);

        assertThatThrownBy(() -> handle.select("select id from something")
            .collectInto(OptionalLong.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Multiple values for optional");
    }

    @Test
    public void testMapToOptionalDouble() throws Exception {
        assertThat(handle.select("select id from something where name = 'slartibartfast'")
            .collectInto(OptionalDouble.class))
            .isEmpty();

        assertThat(handle.select("select null from something where name = 'eric'")
            .collectInto(OptionalDouble.class))
            .isEmpty();

        assertThat(handle.select("select id from something where name = 'eric'")
            .collectInto(OptionalDouble.class))
            .hasValue(1d);

        assertThatThrownBy(() -> handle.select("select id from something")
            .collectInto(OptionalDouble.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Multiple values for optional");
    }

    @Test
    public void testDynamicBindOptionalPresent() throws Exception {
        Something result = handle.createQuery(SELECT_BY_NAME)
                .bindByType("name", Optional.of("eric"), new GenericType<Optional<String>>() {})
                .mapToBean(Something.class)
                .findOnly();

        assertThat(result).isEqualTo(new Something(1, "eric"));
    }

    @Test
    public void testDynamicBindOptionalEmpty() throws Exception {
        List<Something> result = handle.createQuery(SELECT_BY_NAME)
                .bindByType("name", Optional.empty(), new GenericType<Optional<String>>() {})
                .mapToBean(Something.class)
                .list();

        assertThat(result).containsExactly(new Something(1, "eric"), new Something(2, "brian"));
    }

    @Test
    public void testDynamicBindOptionalOfCustomType() throws Exception {
        handle.registerArgument(new NameArgumentFactory());
        handle.createQuery(SELECT_BY_NAME)
                .bindByType("name", Optional.of(new Name("eric")), new GenericType<Optional<Name>>() {})
                .mapToBean(Something.class)
                .list();
    }

    @Test
    public void testDynamicBindOptionalOfUnregisteredCustomType() throws Exception {
        exception.expect(UnsupportedOperationException.class);
        handle.createQuery(SELECT_BY_NAME)
                .bindByType("name", Optional.of(new Name("eric")), new GenericType<Optional<Name>>() {})
                .mapToBean(Something.class)
                .list();
    }

    @Test
    public void testBindOptionalPresent() throws Exception {
        Something result = handle.createQuery(SELECT_BY_NAME)
                .bind("name", Optional.of("brian"))
                .mapToBean(Something.class)
                .findOnly();

        assertThat(result).isEqualTo(new Something(2, "brian"));
    }

    @Test
    public void testBindOptionalEmpty() throws Exception {
        List<Something> result = handle.createQuery(SELECT_BY_NAME)
                .bind("name", Optional.empty())
                .mapToBean(Something.class)
                .list();

        assertThat(result).containsExactly(new Something(1, "eric"), new Something(2, "brian"));
    }

    @Test
    public void testBindOptionalOfCustomType() throws Exception {
        handle.registerArgument(new NameArgumentFactory());
        List<Something> result = handle.createQuery(SELECT_BY_NAME)
                .bind("name", Optional.of(new Name("eric")))
                .mapToBean(Something.class)
                .list();

        assertThat(result).containsExactly(new Something(1, "eric"));
    }

    @Test
    public void testBindOptionalOfUnregisteredCustomType() throws Exception {
        exception.expect(UnsupportedOperationException.class);
        handle.createQuery(SELECT_BY_NAME)
                .bind("name", Optional.of(new Name("eric")))
                .mapToBean(Something.class)
                .list();
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
        public Optional<Argument> build(Type expectedType, Object value, ConfigRegistry config) {
            if (expectedType == Name.class) {
                Name nameValue = (Name) value;
                return Optional.of((pos, stmt, c) -> stmt.setString(pos, nameValue.value));
            }
            return Optional.empty();
        }
    }

}
