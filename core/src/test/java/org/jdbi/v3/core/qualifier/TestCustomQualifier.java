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
import java.util.Objects;
import java.util.Optional;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.internal.AnnotationFactory;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.mapper.reflect.FieldMapper;
import org.jdbi.v3.core.rule.DatabaseRule;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.StatementContext;
import org.junit.Rule;
import org.junit.Test;

public class TestCustomQualifier {
    private static final Reversed REVERSED = AnnotationFactory.create(Reversed.class);
    private static final UpperCase UPPER_CASE = AnnotationFactory.create(UpperCase.class);

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

    public static class QualifiedGetterThing {
        private int id;
        private String name;

        public QualifiedGetterThing() {}

        QualifiedGetterThing(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        @Reversed
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QualifiedGetterThing that = (QualifiedGetterThing) o;
            return id == that.id &&
                Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }

        @Override
        public String toString() {
            return "QualifiedGetterThing{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
        }
    }

    @Test
    public void bindBeanQualifiedGetter() {
        dbRule.getJdbi()
            .registerArgument(new ReversedStringArgumentFactory())
            .useHandle(handle -> {
                handle.createUpdate("INSERT INTO something (id, name) VALUES (:id, :name)")
                    .bindBean(new QualifiedGetterThing(1, "abc"))
                    .execute();

                assertThat(handle.select("SELECT name FROM something")
                    .mapTo(String.class)
                    .findOnly())
                    .isEqualTo("cba");
            });
    }

    @Test
    public void mapBeanQualifiedGetter() {
        dbRule.getJdbi()
            .registerColumnMapper(new ReversedStringMapper())
            .registerRowMapper(BeanMapper.factory(QualifiedGetterThing.class))
            .useHandle(handle -> {
                handle.execute("INSERT INTO something (id, name) VALUES (1, 'abc')");

                assertThat(handle.select("SELECT * FROM something")
                    .mapTo(QualifiedGetterThing.class)
                    .findOnly())
                    .isEqualTo(new QualifiedGetterThing(1, "cba"));
            });
    }

    public static class QualifiedSetterThing {
        private int id;
        private String name;

        public QualifiedSetterThing() {}

        QualifiedSetterThing(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        @Reversed
        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QualifiedSetterThing that = (QualifiedSetterThing) o;
            return id == that.id &&
                Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }

        @Override
        public String toString() {
            return "QualifiedSetterThing{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
        }
    }

    @Test
    public void bindBeanQualifiedSetter() {
        dbRule.getJdbi()
            .registerArgument(new ReversedStringArgumentFactory())
            .useHandle(handle -> {
                handle.createUpdate("INSERT INTO something (id, name) VALUES (:id, :name)")
                    .bindBean(new QualifiedSetterThing(1, "abc"))
                    .execute();

                assertThat(handle.select("SELECT name FROM something")
                    .mapTo(String.class)
                    .findOnly())
                    .isEqualTo("cba");
            });
    }

    @Test
    public void mapBeanQualifiedSetter() {
        dbRule.getJdbi()
            .registerColumnMapper(new ReversedStringMapper())
            .registerRowMapper(BeanMapper.factory(QualifiedSetterThing.class))
            .useHandle(handle -> {
                handle.execute("INSERT INTO something (id, name) VALUES (1, 'abc')");

                assertThat(handle.select("SELECT * FROM something")
                    .mapTo(QualifiedSetterThing.class)
                    .findOnly())
                    .isEqualTo(new QualifiedSetterThing(1, "cba"));
            });
    }

    public static class QualifiedSetterParamThing {
        private int id;
        private String name;

        public QualifiedSetterParamThing() {}

        QualifiedSetterParamThing(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(@Reversed String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QualifiedSetterParamThing that = (QualifiedSetterParamThing) o;
            return id == that.id &&
                Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }

        @Override
        public String toString() {
            return "QualifiedSetterParamThing{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
        }
    }

    @Test
    public void bindBeanQualifiedSetterParam() {
        dbRule.getJdbi()
            .registerArgument(new ReversedStringArgumentFactory())
            .useHandle(handle -> {
                handle.createUpdate("INSERT INTO something (id, name) VALUES (:id, :name)")
                    .bindBean(new QualifiedSetterParamThing(1, "abc"))
                    .execute();

                assertThat(handle.select("SELECT name FROM something")
                    .mapTo(String.class)
                    .findOnly())
                    .isEqualTo("cba");
            });
    }

    @Test
    public void mapBeanQualifiedSetterParam() {
        dbRule.getJdbi()
            .registerColumnMapper(new ReversedStringMapper())
            .registerRowMapper(BeanMapper.factory(QualifiedSetterParamThing.class))
            .useHandle(handle -> {
                handle.execute("INSERT INTO something (id, name) VALUES (1, 'abc')");

                assertThat(handle.select("SELECT * FROM something")
                    .mapTo(QualifiedSetterParamThing.class)
                    .findOnly())
                    .isEqualTo(new QualifiedSetterParamThing(1, "cba"));
            });
    }

    public static class QualifiedMethodThing {
        private final int id;
        private final String name;

        QualifiedMethodThing(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int id() {
            return id;
        }

        @Reversed
        public String name() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QualifiedMethodThing that = (QualifiedMethodThing) o;
            return id == that.id &&
                Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }

        @Override
        public String toString() {
            return "QualifiedMethodThing{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
        }
    }

    @Test
    public void bindMethodsQualifiedMethod() {
        dbRule.getJdbi()
            .registerArgument(new ReversedStringArgumentFactory())
            .useHandle(handle -> {
                handle.createUpdate("INSERT INTO something (id, name) VALUES (:id, :name)")
                    .bindMethods(new QualifiedMethodThing(1, "abc"))
                    .execute();

                assertThat(handle.select("SELECT name FROM something")
                    .mapTo(String.class)
                    .findOnly())
                    .isEqualTo("cba");
            });
    }

    public static class QualifiedConstructorParamThing {
        private final int id;
        private final String name;

        public QualifiedConstructorParamThing(int id, @Reversed String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QualifiedConstructorParamThing that = (QualifiedConstructorParamThing) o;
            return id == that.id &&
                Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }

        @Override
        public String toString() {
            return "QualifiedConstructorParamThing{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
        }
    }

    @Test
    public void mapConstructorQualifiedParam() {
        dbRule.getJdbi()
            .registerColumnMapper(new ReversedStringMapper())
            .registerRowMapper(ConstructorMapper.factory(QualifiedConstructorParamThing.class))
            .useHandle(handle -> {
                handle.execute("INSERT INTO something (id, name) VALUES (1, 'abc')");

                assertThat(handle.select("SELECT * FROM something")
                    .mapTo(QualifiedConstructorParamThing.class)
                    .findOnly())
                    .isEqualTo(new QualifiedConstructorParamThing(1, "cba"));
            });
    }

    public static class QualifiedFieldThing {
        public int id;

        @Reversed
        public String name;

        public QualifiedFieldThing() {}

        QualifiedFieldThing(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QualifiedFieldThing that = (QualifiedFieldThing) o;
            return id == that.id &&
                Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }

        @Override
        public String toString() {
            return "QualifiedFieldThing{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
        }
    }

    @Test
    public void bindFieldsQualified() {
        dbRule.getJdbi()
            .registerArgument(new ReversedStringArgumentFactory())
            .useHandle(handle -> {
                handle.createUpdate("INSERT INTO something (id, name) VALUES (:id, :name)")
                    .bindFields(new QualifiedFieldThing(1, "abc"))
                    .execute();

                assertThat(handle.select("SELECT name FROM something")
                    .mapTo(String.class)
                    .findOnly())
                    .isEqualTo("cba");
            });
    }

    @Test
    public void mapFieldsQualified() {
        dbRule.getJdbi()
            .registerColumnMapper(new ReversedStringMapper())
            .registerRowMapper(FieldMapper.factory(QualifiedFieldThing.class))
            .useHandle(handle -> {
                handle.execute("INSERT INTO something (id, name) VALUES (1, 'abc')");

                assertThat(handle.select("SELECT * FROM something")
                    .mapTo(QualifiedFieldThing.class)
                    .findOnly())
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
            return (pos, stmt, ctx) -> stmt.setString(pos, value.toUpperCase());
        }
    }

    @UpperCase
    static class UpperCaseStringMapper implements ColumnMapper<String> {
        @Override
        public String map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            return r.getString(columnNumber).toUpperCase();
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
            return (pos, stmt, ctx) -> stmt.setString(pos, reverse(value).toUpperCase());
        }
    }

    @Reversed
    @UpperCase
    static class ReversedUpperCaseStringMapper implements ColumnMapper<String> {
        @Override
        public String map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            return reverse(r.getString(columnNumber)).toUpperCase();
        }
    }

    @Test
    public void bindMultipleQualifiers() {
        dbRule.getJdbi()
            // should use this one - register first so it's consulted last
            .registerArgument(new ReversedUpperCaseStringArgumentFactory())
            .registerArgument(new ReversedStringArgumentFactory())
            .registerArgument(new UpperCaseArgumentFactory())
            .useHandle(handle -> {
                handle.createUpdate("INSERT INTO something (id, name) VALUES (1, :name)")
                    .bindByType("name", "abc", QualifiedType.of(String.class, REVERSED, UPPER_CASE))
                    .execute();

                assertThat(handle.select("SELECT name FROM something")
                    .mapTo(String.class)
                    .findOnly())
                    .isEqualTo("CBA");
            });
    }

    @Test
    public void mapMultipleQualifiers() {
        dbRule.getJdbi()
            // should use this one - register first so it's consulted last
            .registerColumnMapper(new ReversedUpperCaseStringMapper())
            .registerColumnMapper(new ReversedStringMapper())
            .registerColumnMapper(new UpperCaseStringMapper())
            .useHandle(handle -> {
                handle.execute("INSERT INTO something (id, name) VALUES (1, 'abc')");

                assertThat(handle.select("SELECT name FROM something")
                    .mapTo(String.class, REVERSED, UPPER_CASE)
                    .findOnly())
                    .isEqualTo("CBA");
            });
    }
}
