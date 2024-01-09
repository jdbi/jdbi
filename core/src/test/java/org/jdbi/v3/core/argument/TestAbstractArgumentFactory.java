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
package org.jdbi.v3.core.argument;

import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Optional;
import java.util.function.Function;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementContextAccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TestAbstractArgumentFactory {
    ConfigRegistry config = new ConfigRegistry();

    @Mock
    PreparedStatement statement;

    StatementContext ctx = StatementContextAccess.createContext();

    static class SimpleType {
        final String value;

        SimpleType(String value) {
            this.value = value;
        }
    }

    static class SimpleArgumentFactory extends AbstractArgumentFactory<SimpleType> {
        SimpleArgumentFactory() {
            super(Types.VARCHAR);
        }

        @Override
        protected Argument build(SimpleType value, ConfigRegistry config) {
            return (pos, stmt, ctx) -> stmt.setString(pos, value.value);
        }
    }

    @Test
    public void testExpectedClass() throws SQLException {
        Argument argument = new SimpleArgumentFactory().build(SimpleType.class, new SimpleType("foo"), config).orElse(null);
        assertThat(argument).isNotNull();

        argument.apply(1, statement, ctx);
        verify(statement).setString(1, "foo");
    }

    @Test
    public void testObjectClassWithInstanceOfExpectedType() throws SQLException {
        Argument argument = new SimpleArgumentFactory().build(Object.class, new SimpleType("bar"), config).orElse(null);
        assertThat(argument).isNotNull();

        argument.apply(2, statement, ctx);
        verify(statement).setString(2, "bar");
    }

    @Test
    public void testNullOfExpectedClass() throws SQLException {
        Argument argument = new SimpleArgumentFactory().build(SimpleType.class, null, config).orElse(null);
        assertThat(argument).isNotNull();

        argument.apply(3, statement, ctx);
        verify(statement).setNull(3, Types.VARCHAR);
    }

    @Test
    public void testValueOfDifferentType() {
        assertThat(new SimpleArgumentFactory().build(int.class, 1, config)).isEmpty();
    }

    @Test
    public void testNullOfDifferentType() {
        assertThat(new SimpleArgumentFactory().build(Integer.class, null, config)).isEmpty();
    }

    static class Box<T> {
        final T value;

        Box(T value) {
            this.value = value;
        }
    }

    static class BoxOfStringArgumentFactory extends AbstractArgumentFactory<Box<String>> {
        BoxOfStringArgumentFactory() {
            super(Types.VARCHAR);
        }

        @Override
        protected Argument build(Box<String> value, ConfigRegistry config) {
            return (pos, stmt, ctx) -> stmt.setString(pos, value.value);
        }
    }

    private static final Type BOX_OF_STRING_TYPE = new GenericType<Box<String>>() {}.getType();
    private static final Type BOX_OF_OBJECT_TYPE = new GenericType<Box<Object>>() {}.getType();

    @Test
    public void testExpectedGenericType() throws SQLException {
        Argument argument = new BoxOfStringArgumentFactory().build(BOX_OF_STRING_TYPE, new Box<>("foo"), config).orElse(null);
        assertThat(argument).isNotNull();

        argument.apply(1, statement, ctx);
        verify(statement).setString(1, "foo");
    }

    @Test
    public void testExpectedGenericTypeWithDifferentParameter() {
        assertThat(new BoxOfStringArgumentFactory().build(BOX_OF_OBJECT_TYPE, new Box<Object>("foo"), config))
                .isEmpty();
    }

    @Test
    public void testNullOfExpectedGenericType() throws SQLException {
        Argument argument = new BoxOfStringArgumentFactory().build(BOX_OF_STRING_TYPE, null, config).orElse(null);
        assertThat(argument).isNotNull();

        argument.apply(2, statement, ctx);
        verify(statement).setNull(2, Types.VARCHAR);
    }

    static class StringBox extends Box<String> {

        StringBox(String value) {
            super(value);
        }
    }

    static class IntegerBox extends Box<Integer> {

        IntegerBox(Integer value) {
            super(value);
        }
    }

    @Test
    public void testConcreteClassMatches() {
        ArgumentFactory.Preparable preparable = new BoxOfStringArgumentFactory();
        Optional<Function<Object, Argument>> argument = preparable.prepare(StringBox.class, new ConfigRegistry());
        assertThat(argument).isPresent();

        argument = preparable.prepare(IntegerBox.class, new ConfigRegistry());
        assertThat(argument).isNotPresent();
    }

    @Test
    public void testGenericClassMatches() {
        ConfigRegistry registry = new ConfigRegistry();
        Arguments arguments = new Arguments(registry);
        arguments.register(new BoxOfStringArgumentFactory());
        Box<String> genericBoxString = new Box<>("foo");
        StringBox stringBox = new StringBox("bar");

        assertThat(arguments.findFor(QualifiedType.of(new GenericType<Box<String>>() {}), genericBoxString)).isPresent();
        assertThat(arguments.findFor(QualifiedType.of(new GenericType<Box<String>>() {}), stringBox)).isPresent();
        assertThat(arguments.findFor(StringBox.class, stringBox)).isPresent();
    }

    @Test
    public void testGenericClassNonMatches() {
        ConfigRegistry registry = new ConfigRegistry();
        Arguments arguments = new Arguments(registry);
        arguments.register(new BoxOfStringArgumentFactory());
        Box<Integer> genericBoxInteger = new Box<>(10);
        IntegerBox integerBox = new IntegerBox(20);

        assertThat(arguments.findFor(QualifiedType.of(new GenericType<Box<Integer>>() {}), genericBoxInteger)).isNotPresent();
        assertThat(arguments.findFor(QualifiedType.of(new GenericType<Box<Integer>>() {}), integerBox)).isNotPresent();
        assertThat(arguments.findFor(IntegerBox.class, integerBox)).isNotPresent();
    }

    interface Thing<T> {

        T getKey();
    }

    static class StringThing implements Thing<String> {

        private final String key;

        StringThing(final String key) {
            this.key = key;
        }

        @Override
        public String getKey() {
            return this.key;
        }
    }

    static class IntegerThing implements Thing<Integer> {

        private final Integer key;

        IntegerThing(final Integer key) {
            this.key = key;
        }

        @Override
        public Integer getKey() {
            return this.key;
        }
    }

    public static class StringThingArgumentFactory extends AbstractArgumentFactory<Thing<String>> {

        protected StringThingArgumentFactory() {
            super(Types.VARCHAR);
        }

        @Override
        protected Argument build(final Thing<String> value, final ConfigRegistry config) {
            return ((position, statement, ctx1) -> statement.setString(position, value.getKey()));
        }
    }

    @Test
    public void testGenericInterfaceMatches() {
        ConfigRegistry registry = new ConfigRegistry();
        Arguments arguments = new Arguments(registry);
        arguments.register(new StringThingArgumentFactory());
        StringThing stringThing = new StringThing("bar");

        assertThat(arguments.findFor(QualifiedType.of(new GenericType<Thing<String>>() {}), stringThing)).isPresent();
        assertThat(arguments.findFor(StringThing.class, stringThing)).isPresent();
    }

    @Test
    public void testGenericInterfaceNonMatches() {
        ConfigRegistry registry = new ConfigRegistry();
        Arguments arguments = new Arguments(registry);
        arguments.register(new StringThingArgumentFactory());
        IntegerThing integerThing = new IntegerThing(20);

        assertThat(arguments.findFor(QualifiedType.of(new GenericType<Thing<Integer>>() {}), integerThing)).isNotPresent();
        assertThat(arguments.findFor(IntegerThing.class, integerThing)).isNotPresent();
    }
}
