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
package org.jdbi.v3;

import org.jdbi.v3.exceptions.UnableToCreateStatementException;
import org.jdbi.v3.tweak.Argument;
import org.jdbi.v3.tweak.ArgumentFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

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
        handle.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        handle.createStatement("insert into something (id, name) values (2, 'brian')").execute();
    }

    @Test
    public void testDynamicBindOptionalPresent() throws Exception {
        Something result = handle.createQuery(SELECT_BY_NAME)
                .bindByType("name", Optional.of("eric"), new GenericType<Optional<String>>() {})
                .mapToBean(Something.class)
                .findOnly();

        assertEquals(1, result.getId());
        assertEquals("eric", result.getName());
    }

    @Test
    public void testDynamicBindOptionalEmpty() throws Exception {
        List<Something> result = handle.createQuery(SELECT_BY_NAME)
                .bindByType("name", Optional.empty(), new GenericType<Optional<String>>() {})
                .mapToBean(Something.class)
                .list();

        assertThat(result.size(), equalTo(2));

        assertEquals(1, result.get(0).getId());
        assertEquals("eric", result.get(0).getName());

        assertEquals(2, result.get(1).getId());
        assertEquals("brian", result.get(1).getName());
    }

    @Test
    public void testDynamicBindOptionalOfCustomType() throws Exception {
        handle.registerArgumentFactory(new NameArgumentFactory());
        handle.createQuery(SELECT_BY_NAME)
                .bindByType("name", Optional.of(new Name("eric")), new GenericType<Optional<Name>>() {})
                .mapToBean(Something.class)
                .list();
    }

    @Test
    public void testDynamicBindOptionalOfUnregisteredCustomType() throws Exception {
        exception.expect(UnableToCreateStatementException.class);
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

        assertEquals(2, result.getId());
        assertEquals("brian", result.getName());
    }

    @Test
    public void testBindOptionalEmpty() throws Exception {
        List<Something> result = handle.createQuery(SELECT_BY_NAME)
                .bind("name", Optional.empty())
                .mapToBean(Something.class)
                .list();

        assertThat(result.size(), equalTo(2));

        assertEquals(1, result.get(0).getId());
        assertEquals("eric", result.get(0).getName());

        assertEquals(2, result.get(1).getId());
        assertEquals("brian", result.get(1).getName());
    }

    @Test
    public void testBindOptionalOfCustomType() throws Exception {
        handle.registerArgumentFactory(new NameArgumentFactory());
        List<Something> result = handle.createQuery(SELECT_BY_NAME)
                .bind("name", Optional.of(new Name("eric")))
                .mapToBean(Something.class)
                .list();

        assertThat(result.size(), equalTo(1));

        assertEquals(1, result.get(0).getId());
        assertEquals("eric", result.get(0).getName());
    }

    @Test
    public void testBindOptionalOfUnregisteredCustomType() throws Exception {
        exception.expect(UnableToCreateStatementException.class);
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
        public Optional<Argument> build(Type expectedType, Object value, StatementContext ctx) {
            if (expectedType == Name.class) {
                Name nameValue = (Name) value;
                return Optional.of((pos, stmt, c) -> stmt.setString(pos, nameValue.value));
            }
            return Optional.empty();
        }
    }

}
