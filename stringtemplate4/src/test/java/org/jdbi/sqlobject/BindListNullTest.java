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
package org.jdbi.sqlobject;

import java.util.ArrayList;
import java.util.List;

import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.Something;
import org.jdbi.core.mapper.SomethingMapper;
import org.jdbi.core.statement.ColonPrefixSqlParser;
import org.jdbi.core.statement.ParsedSql;
import org.jdbi.core.statement.SqlParser;
import org.jdbi.core.statement.StatementContext;
import org.jdbi.sqlobject.customizer.BindList;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit5.JdbiExtension;
import org.jdbi.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.sqlobject.customizer.BindList.EmptyHandling.NULL_STRING;
import static org.jdbi.sqlobject.customizer.BindList.EmptyHandling.VOID;

public class BindListNullTest {
    private Handle handle;

    @RegisterExtension
    public final JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something());

    @BeforeEach
    public void before() {
        final Jdbi db = h2Extension.getJdbi();
        db.registerRowMapper(new SomethingMapper());
        db.installPlugin(new SqlObjectPlugin());
        handle = db.open();

        handle.execute("insert into something(id, name) values(1, null)");
        handle.execute("insert into something(id, name) values(2, null)");
        handle.execute("insert into something(id, name) values(3, null)");
        handle.execute("insert into something(id, name) values(4, null)");

        handle.execute("insert into something(id, name) values(5, 'bla')");
        handle.execute("insert into something(id, name) values(6, 'null')");
        handle.execute("insert into something(id, name) values(7, '')");
    }

    @AfterEach
    public void after() {
        handle.close();
    }

    @Test
    public void testSomethingByIterableHandleNullWithNull() {
        final SomethingByIterableHandleNull s = handle.attach(SomethingByIterableHandleNull.class);

        final List<Something> out = s.get(null);

        assertThat(out).isEmpty();
    }

    @Test
    public void testSomethingByIterableHandleNullWithEmptyList() {
        final SomethingByIterableHandleNull s = handle.attach(SomethingByIterableHandleNull.class);

        final List<Something> out = s.get(new ArrayList<>());

        assertThat(out).isEmpty();
    }

    public interface SomethingByIterableHandleNull {
        @SqlQuery("select id, name from something where name in (<names>)")
        List<Something> get(@BindList(onEmpty = NULL_STRING) Iterable<Object> names);
    }

    //

    @Test
    public void testSomethingByIterableHandleVoidWithNull() {
        final List<String> log = new ArrayList<>();
        handle.setSqlParser(new LoggingParser(log));
        final SomethingByIterableHandleVoid s = handle.attach(SomethingByIterableHandleVoid.class);

        final List<Something> out = s.get(null);

        assertThat(out).isEmpty();
        assertThat(log).hasSize(1).allMatch(e -> e.contains(" where id in ();"));
    }

    @Test
    public void testSomethingByIterableHandleVoidWithEmptyList() {
        final List<String> log = new ArrayList<>();
        handle.setSqlParser(new LoggingParser(log));
        final SomethingByIterableHandleVoid s = handle.attach(SomethingByIterableHandleVoid.class);

        final List<Something> out = s.get(new ArrayList<>());

        assertThat(out).isEmpty();
        assertThat(log).hasSize(1).allMatch(e -> e.contains(" where id in ();"));
    }

    public interface SomethingByIterableHandleVoid {
        @SqlQuery("select id, name from something where id in (<ids>);")
        List<Something> get(@BindList(onEmpty = VOID) Iterable<Object> ids);
    }

    public static class LoggingParser implements SqlParser {
        private final List<String> log;

        public LoggingParser(List<String> log) {
            this.log = log;
        }

        @Override
        public ParsedSql parse(String sql, StatementContext ctx) {
            log.add(sql);
            return new ColonPrefixSqlParser().parse(sql, ctx);
        }

        @Override
        public String nameParameter(String rawName, StatementContext ctx) {
            return new ColonPrefixSqlParser().nameParameter(rawName, ctx);
        }
    }
}
