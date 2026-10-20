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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.qualifier.SampleQualifiers.Foo;
import org.jdbi.v3.core.statement.StatementContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.core.qualifier.SampleQualifiers.foo;

/**
 * A qualifier annotation with members is matched by value, so two factories that differ only in the
 * member value stay distinct. The factory annotations come from reflection and the lookup keys are
 * built by hand, which also exercises the member check under a native image, where the whole core
 * suite runs.
 */
public class TestQualifierWithMembers {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(H2DatabaseExtension.SOMETHING_INITIALIZER);

    private static final QualifiedType<String> FOO_ONE = QualifiedType.of(String.class).with(foo(1));
    private static final QualifiedType<String> FOO_TWO = QualifiedType.of(String.class).with(foo(2));

    @Test
    public void argumentFactoriesAreMatchedByMemberValue() {
        Handle handle = h2Extension.getSharedHandle();
        handle.registerArgument(new SuffixOneArgumentFactory());
        handle.registerArgument(new SuffixTwoArgumentFactory());

        handle.createUpdate("INSERT INTO something (id, name) VALUES (1, :name)")
            .bindByType("name", "abc", FOO_ONE)
            .execute();
        handle.createUpdate("INSERT INTO something (id, name) VALUES (2, :name)")
            .bindByType("name", "abc", FOO_TWO)
            .execute();

        assertThat(handle.select("SELECT name FROM something ORDER BY id").mapTo(String.class).list())
            .containsExactly("abc-1", "abc-2");
    }

    @Test
    public void columnMappersAreMatchedByMemberValue() {
        Handle handle = h2Extension.getSharedHandle();
        handle.registerColumnMapper(new SuffixOneMapper());
        handle.registerColumnMapper(new SuffixTwoMapper());
        handle.execute("INSERT INTO something (id, name) VALUES (1, 'abc')");

        assertThat(handle.select("SELECT name FROM something").mapTo(FOO_ONE).one()).isEqualTo("abc-1");
        assertThat(handle.select("SELECT name FROM something").mapTo(FOO_TWO).one()).isEqualTo("abc-2");
        assertThat(handle.select("SELECT name FROM something").mapTo(String.class).one()).isEqualTo("abc");
    }

    @Foo(1)
    static class SuffixOneArgumentFactory extends SuffixArgumentFactory {
        SuffixOneArgumentFactory() {
            super("-1");
        }
    }

    @Foo(2)
    static class SuffixTwoArgumentFactory extends SuffixArgumentFactory {
        SuffixTwoArgumentFactory() {
            super("-2");
        }
    }

    abstract static class SuffixArgumentFactory extends AbstractArgumentFactory<String> {
        private final String suffix;

        SuffixArgumentFactory(String suffix) {
            super(Types.VARCHAR);
            this.suffix = suffix;
        }

        @Override
        protected Argument build(String value, ConfigRegistry config) {
            return (pos, stmt, ctx) -> stmt.setString(pos, value + suffix);
        }
    }

    @Foo(1)
    static class SuffixOneMapper implements ColumnMapper<String> {
        @Override
        public String map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            return r.getString(columnNumber) + "-1";
        }
    }

    @Foo(2)
    static class SuffixTwoMapper implements ColumnMapper<String> {
        @Override
        public String map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            return r.getString(columnNumber) + "-2";
        }
    }
}
