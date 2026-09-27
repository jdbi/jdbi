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

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import org.jdbi.core.Handle;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.statement.StatementContext;
import org.jdbi.sqlobject.config.RegisterRowMapper;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Extensions attached to one handle and called from several threads must each use their own configuration.
 * The calls are not serialized, so this relies on the H2 connection being thread-safe.
 */
public class TestConcurrentExtensionInvocation {

    private static final int THREADS = 4;
    private static final int CALLS_PER_THREAD = 5_000;

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin());

    private Handle handle;
    private ExecutorService executor;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
        executor = Executors.newFixedThreadPool(THREADS);
    }

    @AfterEach
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void testSqlQueryUsesOwnMapper() throws Exception {
        assertEachCallUsesOwnMapper(DaoA::query, DaoB::query);
    }

    @Test
    public void testDefaultMethodSeesOwnConfig() throws Exception {
        assertEachCallUsesOwnMapper(DaoA::queryThroughHandle, DaoB::queryThroughHandle);
    }

    private void assertEachCallUsesOwnMapper(Function<DaoA, Source> callA, Function<DaoB, Source> callB) throws Exception {
        final DaoA daoA = handle.attach(DaoA.class);
        final DaoB daoB = handle.attach(DaoB.class);

        final List<Future<List<Source>>> futures = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            final boolean useA = i % 2 == 0;
            futures.add(executor.submit(() -> {
                final List<Source> wrong = new ArrayList<>();
                for (int j = 0; j < CALLS_PER_THREAD; j++) {
                    final Source result = useA ? callA.apply(daoA) : callB.apply(daoB);
                    if (result != (useA ? Source.A : Source.B)) {
                        wrong.add(result);
                    }
                }
                return wrong;
            }));
        }

        for (final Future<List<Source>> future : futures) {
            assertThat(future.get()).isEmpty();
        }
    }

    public enum Source {
        A, B
    }

    @RegisterRowMapper(MapperA.class)
    public interface DaoA extends SqlObject {
        @SqlQuery("select 1")
        Source query();

        default Source queryThroughHandle() {
            return getHandle().createQuery("select 1").mapTo(Source.class).one();
        }
    }

    @RegisterRowMapper(MapperB.class)
    public interface DaoB extends SqlObject {
        @SqlQuery("select 1")
        Source query();

        default Source queryThroughHandle() {
            return getHandle().createQuery("select 1").mapTo(Source.class).one();
        }
    }

    public static class MapperA implements RowMapper<Source> {
        @Override
        public Source map(ResultSet rs, StatementContext ctx) {
            return Source.A;
        }
    }

    public static class MapperB implements RowMapper<Source> {
        @Override
        public Source map(ResultSet rs, StatementContext ctx) {
            return Source.B;
        }
    }
}
