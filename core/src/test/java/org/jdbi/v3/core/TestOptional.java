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

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.util.GenericType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.core.util.GenericTypes.getErasedType;

public class TestOptional {
    private static final String SELECT_BY_NAME = "select * from something " +
            "where :name is null or name = :name " +
            "order by id";

    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    Handle handle;

    @Before
    public void createTestData() {
        handle = db.openHandle();
        handle.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        handle.createUpdate("insert into something (id, name) values (2, 'brian')").execute();
    }

    @Test
    public void testDynamicBindOptionalPresent() throws Exception {
        Something result = handle.createQuery(SELECT_BY_NAME)
                .bind("name", Optional.of("eric"), new GenericType<Optional<String>>() {})
                .mapToBean(Something.class)
                .findOnly();

        assertThat(result).isEqualTo(new Something(1, "eric"));
    }

    @Test
    public void testDynamicBindOptionalEmpty() throws Exception {
        List<Something> result = handle.createQuery(SELECT_BY_NAME)
                .bind("name", Optional.empty(), new GenericType<Optional<String>>() {})
                .mapToBean(Something.class)
                .list();

        assertThat(result).containsExactly(new Something(1, "eric"), new Something(2, "brian"));
    }

    @Test
    public void testDynamicBindOptionalOfCustomType() throws Exception {
        handle.registerArgument(new NameArgumentFactory());
        handle.createQuery(SELECT_BY_NAME)
                .bind("name", Optional.of(new Name("eric")), new GenericType<Optional<Name>>() {})
                .mapToBean(Something.class)
                .list();
    }

    @Test
    public void testDynamicBindOptionalOfUnregisteredCustomType() throws Exception {
        exception.expect(UnsupportedOperationException.class);
        handle.createQuery(SELECT_BY_NAME)
                .bind("name", Optional.of(new Name("eric")), new GenericType<Optional<Name>>() {})
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
        public Optional<Argument> build(Type expectedType, ConfigRegistry config) {
            if (Name.class.isAssignableFrom(getErasedType(expectedType))) {
                Argument<Name> arg = (stmt, pos, value, c) -> stmt.setString(pos, value.value);
                return Optional.of(arg);
            }
            return Optional.empty();
        }
    }

}
