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
package org.jdbi.v3.core.qualifier;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Optional;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.internal.AnnotationFactory;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.rule.DatabaseRule;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.StatementContext;
import org.junit.Rule;
import org.junit.Test;

public class TestCustomQualifier {
    private static final Reversed REVERSED = AnnotationFactory.create(Reversed.class);

    @Rule
    public DatabaseRule dbRule = new H2DatabaseRule();

    @Retention(RetentionPolicy.RUNTIME)
    @Qualifier
    @interface Reversed {}

    @Reversed
    static class ReversedStringArgumentFactory extends AbstractArgumentFactory<String> {
        ReversedStringArgumentFactory() {
            super(Types.VARCHAR);
        }

        @Override
        protected Argument build(String value, ConfigRegistry config) {
            return (pos, stmt, ctx) -> stmt.setString(pos, reverse(value));
        }
    }

    @Reversed
    static class ReversedStringMapper implements ColumnMapper<String> {
        @Override
        public String map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            return reverse(r.getString(columnNumber));
        }
    }

    @Reversed
    static class ReversedStringMapperFactory implements ColumnMapperFactory {
        @Override
        public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
            if (String.class.equals(type)) {
                return Optional.of((rs, col, ctx) -> reverse(rs.getString(col)));
            }
            return Optional.empty();
        }
    }

    private static String reverse(String s) {
        StringBuilder b = new StringBuilder(s.length());

        for (int i = s.length() - 1; i >= 0; i--) {
            b.append(s.charAt(i));
        }

        return b.toString();
    }

    @Test
    public void registerArgumentFactory() {
        dbRule.getJdbi()
            .registerArgument(new ReversedStringArgumentFactory())
            .useHandle(handle -> {
                handle.createUpdate("INSERT INTO something (id, name) VALUES (1, :name)")
                    .bindByType("name", "abc", QualifiedType.of(String.class, REVERSED))
                    .execute();

                assertThat(
                    handle.select("SELECT name FROM something")
                        .mapTo(String.class)
                        .findOnly())
                    .isEqualTo("cba");
            });
    }

    @Test
    public void configArgumentsRegister() {
        dbRule.getJdbi()
            .configure(Arguments.class, config -> config.register(new ReversedStringArgumentFactory()))
            .useHandle(handle -> {
                handle.createUpdate("INSERT INTO something (id, name) VALUES (1, :name)")
                    .bindByType("name", "abc", QualifiedType.of(String.class, REVERSED))
                    .execute();

                assertThat(
                    handle.select("SELECT name FROM something")
                        .mapTo(String.class)
                        .findOnly())
                    .isEqualTo("cba");
            });
    }

    @Test
    public void registerColumnMapper() {
        dbRule.getJdbi()
            .registerColumnMapper(new ReversedStringMapper())
            .useHandle(handle -> {
                handle.execute("insert into something (id, name) values (1, 'abc')");

                assertThat(
                    handle.select("SELECT name FROM something")
                        .mapTo(String.class, REVERSED)
                        .findOnly())
                    .isEqualTo("cba");
            });
    }

    @Test
    public void configColumnMappersRegister() {
        dbRule.getJdbi()
            .configure(ColumnMappers.class, config -> config.register(new ReversedStringMapper()))
            .useHandle(handle -> {
                handle.execute("insert into something (id, name) values (1, 'abc')");

                assertThat(
                    handle.select("SELECT name FROM something")
                        .mapTo(String.class, REVERSED)
                        .findOnly())
                    .isEqualTo("cba");
            });
    }

    @Test
    public void registerColumnMapperByQualifiedType() {
        dbRule.getJdbi()
            .registerColumnMapper(
                QualifiedType.of(String.class, REVERSED),
                (r, c, ctx) -> reverse(r.getString(c)))
            .useHandle(handle -> {
                handle.execute("insert into something (id, name) values (1, 'abcdef')");

                assertThat(
                    handle.select("SELECT name FROM something")
                        .mapTo(String.class, REVERSED)
                        .findOnly())
                    .isEqualTo("fedcba");
            });
    }

    @Test
    public void configColumnMappersRegisterByQualifiedType() {
        dbRule.getJdbi()
            .configure(ColumnMappers.class, config -> config.register(
                QualifiedType.of(String.class, REVERSED),
                (r, c, ctx) -> reverse(r.getString(c))))
            .useHandle(handle -> {
                handle.execute("insert into something (id, name) values (1, 'abcdef')");

                assertThat(
                    handle.select("SELECT name FROM something")
                        .mapTo(String.class, REVERSED)
                        .findOnly())
                    .isEqualTo("fedcba");
            });
    }

    @Test
    public void registerColumnMapperFactory() {
        dbRule.getJdbi()
            .registerColumnMapper(new ReversedStringMapperFactory())
            .useHandle(handle -> {
                handle.execute("insert into something (id, name) values (1, 'xyz')");

                assertThat(
                    handle.select("SELECT name FROM something")
                        .mapTo(String.class, REVERSED)
                        .findOnly())
                    .isEqualTo("zyx");
            });
    }

    @Test
    public void configColumnMappersRegisterFactory() {
        dbRule.getJdbi()
            .configure(ColumnMappers.class, config -> config.register(new ReversedStringMapperFactory()))
            .useHandle(handle -> {
                handle.execute("insert into something (id, name) values (1, 'xyz')");

                assertThat(
                    handle.select("SELECT name FROM something")
                        .mapTo(String.class, REVERSED)
                        .findOnly())
                    .isEqualTo("zyx");
            });
    }
}
