package org.jdbi.v3.core.statement;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.argument.LoggableArgument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.rule.DatabaseRule;
import org.jdbi.v3.core.rule.SqliteDatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestSqlLoggerToString {
    private static final String INSERT_POSITIONAL = "insert into foo(bar) values(?)";
    private static final String INSERT_NAMED = "insert into foo(bar) values(:x)";
    private static final String NAME = "x";

    @Rule
    public DatabaseRule db = new SqliteDatabaseRule();

    private Handle handle;
    private MutableObject<String> positional = new MutableObject<>();
    private MutableObject<String> named = new MutableObject<>();

    @Before
    public void before() {
        handle = db.getJdbi().open();

        handle.execute("create table foo(bar binary)");

        handle.setSqlLogger(new SqlLogger() {
            @Override
            public void logBeforeExecution(StatementContext context) {
                context.getBinding().findForPosition(0).ifPresent(value -> positional.setValue(Objects.toString(value)));
                context.getBinding().findForName(NAME, context).ifPresent(value -> named.setValue(Objects.toString(value)));
            }
        });
    }

    @Test
    public void testInt() {
        handle.createUpdate(INSERT_POSITIONAL).bind(0, 1).execute();

        assertThat(positional.getValue()).isEqualTo("1");
    }

    @Test
    public void testString() {
        handle.createUpdate(INSERT_POSITIONAL).bind(0, "herp").execute();

        assertThat(positional.getValue()).isEqualTo("herp");
    }

    @Test
    public void testBean() {
        handle.createUpdate(INSERT_NAMED).bindBean(new StringXBean("herp")).execute();

        assertThat(named.getValue()).isEqualTo("herp");
    }

    @Test
    public void testObjectWithToString() {
        Object x = new Object() {
            @Override
            public String toString() {
                return "I'm an object";
            }
        };

        assertThatThrownBy(() -> handle.createUpdate(INSERT_POSITIONAL).bindByType(0, x, Object.class).execute())
            .hasMessage("No argument factory registered for 'I'm an object' of type class java.lang.Object");
    }

    @Test
    // this is why we need LoggableArgument
    public void testArgument() {
        handle.createUpdate(INSERT_POSITIONAL).bind(0, (position, statement, ctx) -> statement.setString(1, "derp")).execute();

        assertThat(positional.getValue()).isNotEqualTo("derp");
    }

    @Test
    public void testLoggableArgument() {
        handle.createUpdate(INSERT_POSITIONAL).bind(0, new LoggableArgument("derp", (position, statement, ctx) -> statement.setString(1, "derp"))).execute();

        assertThat(positional.getValue()).isEqualTo("derp");
    }

    @Test
    // this is why SqlStatement#toArgument uses LoggableArgument
    public void testFoo() {
        handle.registerArgument(new FooArgumentFactory());

        handle.createUpdate(INSERT_POSITIONAL).bind(0, new Foo()).execute();

        assertThat(positional.getValue()).isEqualTo("I'm a foo");
    }

    public static class StringXBean {
        private final String x;

        private StringXBean(String x) {
            this.x = x;
        }

        public String getX() {
            return x;
        }
    }

    private static class Foo {
        @Override
        public String toString() {
            return "I'm a foo";
        }
    }

    private static class FooArgumentFactory implements ArgumentFactory {
        @Override
        public Optional<Argument> build(Type type, Object value, ConfigRegistry config) {
            if (value instanceof Foo) {
                return Optional.of((position, statement, ctx) -> statement.setString(1, "foo"));
            } else {
                return Optional.empty();
            }
        }
    }
}
