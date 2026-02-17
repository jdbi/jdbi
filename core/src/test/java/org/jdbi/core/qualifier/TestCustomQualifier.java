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
package org.jdbi.core.qualifier;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

import org.jdbi.core.argument.AbstractArgumentFactory;
import org.jdbi.core.argument.Argument;
import org.jdbi.core.argument.Arguments;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.internal.testing.H2DatabaseExtension;
import org.jdbi.core.mapper.ColumnMapper;
import org.jdbi.core.mapper.ColumnMappers;
import org.jdbi.core.mapper.reflect.BeanMapper;
import org.jdbi.core.mapper.reflect.ConstructorMapper;
import org.jdbi.core.mapper.reflect.FieldMapper;
import org.jdbi.core.statement.StatementContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.core.qualifier.Reverser.reverse;

public class TestCustomQualifier {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(H2DatabaseExtension.SOMETHING_INITIALIZER);

    @Test
    public void registerArgumentFactory() {
        h2Extension.getJdbi()
            .registerArgument(new ReversedStringArgumentFactory())
            .useHandle(handle -> {
                handle.createUpdate("INSERT INTO something (id, name) VALUES (1, :name)")
                    .bindByType("name", "abc", QualifiedType.of(String.class).with(Reversed.class))
                    .execute();

                assertThat(
                    handle.select("SELECT name FROM something")
                        .mapTo(String.class)
                        .one())
                    .isEqualTo("cba");
            });
    }

    @Test
    public void configArgumentsRegister() {
        h2Extension.getJdbi()
            .configure(Arguments.class, config -> config.register(new ReversedStringArgumentFactory()))
            .useHandle(handle -> {
                handle.createUpdate("INSERT INTO something (id, name) VALUES (1, :name)")
                    .bindByType("name", "abc", QualifiedType.of(String.class).with(Reversed.class))
                    .execute();

                assertThat(
                    handle.select("SELECT name FROM something")
                        .mapTo(String.class)
                        .one())
                    .isEqualTo("cba");
            });
    }

    @Test
    public void registerColumnMapper() {
        h2Extension.getJdbi()
            .registerColumnMapper(new ReversedStringMapper())
            .useHandle(handle -> {
                handle.execute("insert into something (id, name) values (1, 'abc')");

                assertThat(
                    handle.select("SELECT name FROM something")
                        .mapTo(QualifiedType.of(String.class).with(Reversed.class))
                        .one())
                    .isEqualTo("cba");
            });
    }

    @Test
    public void configColumnMappersRegister() {
        h2Extension.getJdbi()
            .configure(ColumnMappers.class, config -> config.register(new ReversedStringMapper()))
            .useHandle(handle -> {
                handle.execute("insert into something (id, name) values (1, 'abc')");

                assertThat(
                    handle.select("SELECT name FROM something")
                        .mapTo(QualifiedType.of(String.class).with(Reversed.class))
                        .one())
                    .isEqualTo("cba");
            });
    }

    @Test
    public void registerColumnMapperByQualifiedType() {
        h2Extension.getJdbi()
            .registerColumnMapper(
                QualifiedType.of(String.class).with(Reversed.class),
                (r, c, ctx) -> reverse(r.getString(c)))
            .useHandle(handle -> {
                handle.execute("insert into something (id, name) values (1, 'abcdef')");

                assertThat(
                    handle.select("SELECT name FROM something")
                        .mapTo(QualifiedType.of(String.class).with(Reversed.class))
                        .one())
                    .isEqualTo("fedcba");
            });
    }

    @Test
    public void configColumnMappersRegisterByQualifiedType() {
        h2Extension.getJdbi()
            .configure(ColumnMappers.class, config -> config.register(
                QualifiedType.of(String.class).with(Reversed.class),
                (r, c, ctx) -> reverse(r.getString(c))))
            .useHandle(handle -> {
                handle.execute("insert into something (id, name) values (1, 'abcdef')");

                assertThat(
                    handle.select("SELECT name FROM something")
                        .mapTo(QualifiedType.of(String.class).with(Reversed.class))
                        .one())
                    .isEqualTo("fedcba");
            });
    }

    @Test
    public void registerColumnMapperFactory() {
        h2Extension.getJdbi()
            .registerColumnMapper(new ReversedStringMapperFactory())
            .useHandle(handle -> {
                handle.execute("insert into something (id, name) values (1, 'xyz')");

                assertThat(
                    handle.select("SELECT name FROM something")
                        .mapTo(QualifiedType.of(String.class).with(Reversed.class))
                        .one())
                    .isEqualTo("zyx");
            });
    }

    @Test
    public void configColumnMappersRegisterFactory() {
        h2Extension.getJdbi()
            .configure(ColumnMappers.class, config -> config.register(new ReversedStringMapperFactory()))
            .useHandle(handle -> {
                handle.execute("insert into something (id, name) values (1, 'xyz')");

                assertThat(
                    handle.select("SELECT name FROM something")
                        .mapTo(QualifiedType.of(String.class).with(Reversed.class))
                        .one())
                    .isEqualTo("zyx");
            });
    }

    @Test
    public void bindBeanQualifiedGetter() {
        h2Extension.getJdbi()
            .registerArgument(new ReversedStringArgumentFactory())
            .useHandle(handle -> {
                handle.createUpdate("INSERT INTO something (id, name) VALUES (:id, :name)")
                    .bindBean(new QualifiedGetterThing(1, "abc"))
                    .execute();

                assertThat(handle.select("SELECT name FROM something")
                    .mapTo(String.class)
                    .one())
                    .isEqualTo("cba");
            });
    }

    @Test
    public void mapBeanQualifiedGetter() {
        h2Extension.getJdbi()
            .registerColumnMapper(new ReversedStringMapper())
            .registerRowMapper(BeanMapper.factory(QualifiedGetterThing.class))
            .useHandle(handle -> {
                handle.execute("INSERT INTO something (id, name) VALUES (1, 'abc')");

                assertThat(handle.select("SELECT * FROM something")
                    .mapTo(QualifiedGetterThing.class)
                    .one())
                    .isEqualTo(new QualifiedGetterThing(1, "cba"));
            });
    }

    @Test
    public void bindBeanQualifiedSetter() {
        h2Extension.getJdbi()
            .registerArgument(new ReversedStringArgumentFactory())
            .useHandle(handle -> {
                handle.createUpdate("INSERT INTO something (id, name) VALUES (:id, :name)")
                    .bindBean(new QualifiedSetterThing(1, "abc"))
                    .execute();

                assertThat(handle.select("SELECT name FROM something")
                    .mapTo(String.class)
                    .one())
                    .isEqualTo("cba");
            });
    }

    @Test
    public void mapBeanQualifiedSetter() {
        h2Extension.getJdbi()
            .registerColumnMapper(new ReversedStringMapper())
            .registerRowMapper(BeanMapper.factory(QualifiedSetterThing.class))
            .useHandle(handle -> {
                handle.execute("INSERT INTO something (id, name) VALUES (1, 'abc')");

                assertThat(handle.select("SELECT * FROM something")
                    .mapTo(QualifiedSetterThing.class)
                    .one())
                    .isEqualTo(new QualifiedSetterThing(1, "cba"));
            });
    }

    @Test
    public void bindBeanQualifiedSetterParam() {
        h2Extension.getJdbi()
            .registerArgument(new ReversedStringArgumentFactory())
            .useHandle(handle -> {
                handle.createUpdate("INSERT INTO something (id, name) VALUES (:id, :name)")
                    .bindBean(new QualifiedSetterParamThing(1, "abc"))
                    .execute();

                assertThat(handle.select("SELECT name FROM something")
                    .mapTo(String.class)
                    .one())
                    .isEqualTo("cba");
            });
    }

    @Test
    public void mapBeanQualifiedSetterParam() {
        h2Extension.getJdbi()
            .registerColumnMapper(new ReversedStringMapper())
            .registerRowMapper(BeanMapper.factory(QualifiedSetterParamThing.class))
            .useHandle(handle -> {
                handle.execute("INSERT INTO something (id, name) VALUES (1, 'abc')");

                assertThat(handle.select("SELECT * FROM something")
                    .mapTo(QualifiedSetterParamThing.class)
                    .one())
                    .isEqualTo(new QualifiedSetterParamThing(1, "cba"));
            });
    }

    @Test
    public void bindMethodsQualifiedMethod() {
        h2Extension.getJdbi()
            .registerArgument(new ReversedStringArgumentFactory())
            .useHandle(handle -> {
                handle.createUpdate("INSERT INTO something (id, name) VALUES (:id, :name)")
                    .bindMethods(new QualifiedMethodThing(1, "abc"))
                    .execute();

                assertThat(handle.select("SELECT name FROM something")
                    .mapTo(String.class)
                    .one())
                    .isEqualTo("cba");
            });
    }

    @Test
    public void mapConstructorQualifiedParam() {
        h2Extension.getJdbi()
            .registerColumnMapper(new ReversedStringMapper())
            .registerRowMapper(ConstructorMapper.factory(QualifiedConstructorParamThing.class))
            .useHandle(handle -> {
                handle.execute("INSERT INTO something (id, name) VALUES (1, 'abc')");

                assertThat(handle.select("SELECT * FROM something")
                    .mapTo(QualifiedConstructorParamThing.class)
                    .one())
                    .isEqualTo(new QualifiedConstructorParamThing(1, "cba"));
            });
    }

    @Test
    public void bindFieldsQualified() {
        h2Extension.getJdbi()
            .registerArgument(new ReversedStringArgumentFactory())
            .useHandle(handle -> {
                handle.createUpdate("INSERT INTO something (id, name) VALUES (:id, :name)")
                    .bindFields(new QualifiedFieldThing(1, "abc"))
                    .execute();

                assertThat(handle.select("SELECT name FROM something")
                    .mapTo(String.class)
                    .one())
                    .isEqualTo("cba");
            });
    }

    @Test
    public void mapFieldsQualified() {
        h2Extension.getJdbi()
            .registerColumnMapper(new ReversedStringMapper())
            .registerRowMapper(FieldMapper.factory(QualifiedFieldThing.class))
            .useHandle(handle -> {
                handle.execute("INSERT INTO something (id, name) VALUES (1, 'abc')");

                assertThat(handle.select("SELECT * FROM something")
                    .mapTo(QualifiedFieldThing.class)
                    .one())
                    .isEqualTo(new QualifiedFieldThing(1, "cba"));
            });
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Qualifier
    @interface UpperCase {}

    @UpperCase
    static class UpperCaseArgumentFactory extends AbstractArgumentFactory<String> {

        UpperCaseArgumentFactory() {
            super(Types.VARCHAR);
        }

        @Override
        protected Argument build(String value, ConfigRegistry config) {
            return (pos, stmt, ctx) -> stmt.setString(pos, value.toUpperCase(Locale.ROOT));
        }
    }

    @UpperCase
    static class UpperCaseStringMapper implements ColumnMapper<String> {

        @Override
        public String map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            return r.getString(columnNumber).toUpperCase(Locale.ROOT);
        }
    }

    @Reversed
    @UpperCase
    static class ReversedUpperCaseStringArgumentFactory extends AbstractArgumentFactory<String> {

        ReversedUpperCaseStringArgumentFactory() {
            super(Types.VARCHAR);
        }

        @Override
        protected Argument build(String value, ConfigRegistry config) {
            return (pos, stmt, ctx) -> stmt.setString(pos, reverse(value).toUpperCase(Locale.ROOT));
        }
    }

    @Reversed
    @UpperCase
    static class ReversedUpperCaseStringMapper implements ColumnMapper<String> {

        @Override
        public String map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            return reverse(r.getString(columnNumber)).toUpperCase(Locale.ROOT);
        }
    }

    @Test
    public void bindMultipleQualifiers() {
        h2Extension.getJdbi()
            // should use this one - register first so it's consulted last
            .registerArgument(new ReversedUpperCaseStringArgumentFactory())
            .registerArgument(new ReversedStringArgumentFactory())
            .registerArgument(new UpperCaseArgumentFactory())
            .useHandle(handle -> {
                handle.createUpdate("INSERT INTO something (id, name) VALUES (1, :name)")
                    .bindByType("name", "abc", QualifiedType.of(String.class).with(Reversed.class, UpperCase.class))
                    .execute();

                assertThat(handle.select("SELECT name FROM something")
                    .mapTo(String.class)
                    .one())
                    .isEqualTo("CBA");
            });
    }

    @Test
    public void mapMultipleQualifiers() {
        h2Extension.getJdbi()
            // should use this one - register first so it's consulted last
            .registerColumnMapper(new ReversedUpperCaseStringMapper())
            .registerColumnMapper(new ReversedStringMapper())
            .registerColumnMapper(new UpperCaseStringMapper())
            .useHandle(handle -> {
                handle.execute("INSERT INTO something (id, name) VALUES (1, 'abc')");

                assertThat(handle.select("SELECT name FROM something")
                    .mapTo(QualifiedType.of(String.class).with(Reversed.class, UpperCase.class))
                    .one())
                    .isEqualTo("CBA");
            });
    }
}
