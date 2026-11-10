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
package org.jdbi.v3.core.statement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.internal.exceptions.Sneaky;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.NoResultsException;
import org.jdbi.v3.core.result.ResultSetException;
import org.jdbi.v3.core.result.RowView;
import org.jdbi.v3.core.result.UnableToProduceResultException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestStatementFailure {
    private static final String SELECT = "select id, name from something order by id";

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(H2DatabaseExtension.SOMETHING_INITIALIZER);

    private final IllegalStateException boom = new IllegalStateException("boom");
    private final ParseException checkedBoom = new ParseException("boom", 0);
    private final List<Exception> failuresAtClose = new ArrayList<>();
    private Handle handle;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
        handle.execute("insert into something (id, name) values (1, 'Zebra'), (2, 'Bananas')");
        // A cleanable added at creation runs when the context closes, as the cleanable of a trace span does.
        // The tests do not close their statements, so that only Jdbi can close the contexts.
        handle.getConfig(SqlStatements.class).addContextListener(new StatementContextListener() {
            @Override
            public void contextCreated(final StatementContext ctx) {
                ctx.addCleanable(() -> failuresAtClose.add(ctx.getFailure()));
            }
        });
    }

    @Test
    public void success() {
        assertThat(handle.createQuery(SELECT).map((rs, ctx) -> rs.getString("name")).list()).containsExactly("Zebra", "Bananas");

        assertThat(failuresAtClose).containsExactly((Exception) null);
    }

    @Test
    public void mapperThrowsSqlException() {
        assertThatThrownBy(() -> handle.createQuery(SELECT).map((rs, ctx) -> rs.getString("missing")).list())
            .isInstanceOf(ResultSetException.class)
            .satisfies(e -> assertThat(failuresAtClose).singleElement().isSameAs(e.getCause()))
            .cause().isInstanceOf(SQLException.class);
    }

    @Test
    public void mapperThrowsOnLastRow() {
        assertThatThrownBy(() -> handle.createQuery(SELECT).map((rs, ctx) -> {
            if (rs.getInt("id") == 2) {
                throw boom;
            }
            return rs.getString("name");
        }).list()).isSameAs(boom);

        assertThat(failuresAtClose).containsExactly(boom);
    }

    @Test
    public void mapperThrowsBeforeLastRow() {
        assertThatThrownBy(() -> handle.createQuery(SELECT).map((rs, ctx) -> {
            throw boom;
        }).list()).isSameAs(boom);

        assertThat(failuresAtClose).containsExactly(boom);
    }

    @Test
    public void noResults() {
        assertThatThrownBy(() -> handle.createQuery("commit").mapTo(Integer.class).findFirst())
            .isInstanceOf(NoResultsException.class)
            .satisfies(e -> assertThat(failuresAtClose).containsExactly((Exception) e));
    }

    @Test
    public void mapperSpecializeThrows() {
        final RowMapper<String> mapper = new RowMapper<>() {
            @Override
            public String map(final ResultSet rs, final StatementContext ctx) throws SQLException {
                return rs.getString("name");
            }

            @Override
            public RowMapper<String> specialize(final ResultSet rs, final StatementContext ctx) {
                throw boom;
            }
        };

        assertThatThrownBy(() -> handle.createQuery(SELECT).map(mapper).one()).isSameAs(boom);

        assertThat(failuresAtClose).containsExactly(boom);
    }

    @Test
    public void mapperSpecializeThrowsCheckedException() {
        final RowMapper<String> mapper = new RowMapper<>() {
            @Override
            public String map(final ResultSet rs, final StatementContext ctx) throws SQLException {
                return rs.getString("name");
            }

            @Override
            public RowMapper<String> specialize(final ResultSet rs, final StatementContext ctx) {
                throw Sneaky.throwAnyway(checkedBoom);
            }
        };

        assertThatThrownBy(() -> handle.createQuery(SELECT).map(mapper).one()).isSameAs(checkedBoom);

        assertThat(failuresAtClose).containsExactly(checkedBoom);
    }

    @Test
    public void constructorMapperWithoutColumn() {
        assertThatThrownBy(() -> handle.createQuery(SELECT).map(ConstructorMapper.of(MissingColumn.class)).list())
            .isInstanceOf(IllegalArgumentException.class)
            .satisfies(e -> assertThat(failuresAtClose).containsExactly((Exception) e));
    }

    @Test
    public void constructorThrowsCheckedException() {
        assertThatThrownBy(() -> handle.createQuery(SELECT).map(ConstructorMapper.of(Unparseable.class)).list())
            .isInstanceOf(ParseException.class)
            .satisfies(e -> assertThat(failuresAtClose).containsExactly((Exception) e));
    }

    @Test
    public void scannerThrowsCheckedException() {
        try (Query query = handle.createQuery(SELECT)) {
            assertThatThrownBy(() -> query.scanResultSet((resultSetSupplier, ctx) -> {
                resultSetSupplier.get();
                throw Sneaky.throwAnyway(checkedBoom);
            })).isSameAs(checkedBoom);
        }

        assertThat(failuresAtClose).containsExactly(checkedBoom);
    }

    @Test
    public void reduceRowsWithSeed() {
        assertThatThrownBy(() -> handle.createQuery(SELECT).reduceRows(0, (count, rowView) -> {
            throw boom;
        })).isSameAs(boom);

        assertThat(failuresAtClose).containsExactly(boom);
    }

    @Test
    public void reduceRowsThrowsCheckedException() {
        assertThatThrownBy(() -> handle.createQuery(SELECT).reduceRows(0, (count, rowView) -> {
            throw Sneaky.throwAnyway(checkedBoom);
        })).isSameAs(checkedBoom);

        assertThat(failuresAtClose).containsExactly(checkedBoom);
    }

    @Test
    public void reduceRowsWithReducer() {
        assertThatThrownBy(() -> handle.createQuery(SELECT).reduceRows((LinkedHashMapRowReducer<Integer, String>) (map, rowView) -> {
            throw boom;
        })).isSameAs(boom);

        assertThat(failuresAtClose).containsExactly(boom);
    }

    @Test
    public void reduceResultSet() {
        assertThatThrownBy(() -> handle.createQuery(SELECT).reduceResultSet(0, (count, rs, ctx) -> rs.getInt("missing")))
            .isInstanceOf(UnableToProduceResultException.class)
            .satisfies(e -> assertThat(failuresAtClose).singleElement().isSameAs(e.getCause()))
            .cause().isInstanceOf(SQLException.class);
    }

    @Test
    public void collectRows() {
        final Collector<RowView, ?, List<String>> failing = Collector.of(ArrayList::new,
            (list, rowView) -> {
                throw boom;
            },
            (left, right) -> left);

        assertThatThrownBy(() -> handle.createQuery(SELECT).collectRows(failing)).isSameAs(boom);

        assertThat(failuresAtClose).containsExactly(boom);
    }

    @Test
    public void reduceRowsCountsRowsBeforeFailure() {
        final List<Long> rowsAtClose = new ArrayList<>();
        handle.getConfig(SqlStatements.class).addContextListener(new StatementContextListener() {
            @Override
            public void contextCreated(final StatementContext ctx) {
                ctx.addCleanable(() -> rowsAtClose.add(ctx.getMappedRows()));
            }
        });

        assertThatThrownBy(() -> handle.createQuery(SELECT).reduceRows(0, (count, rowView) -> {
            if (count == 1) {
                throw boom;
            }
            return count + 1;
        })).isSameAs(boom);

        assertThat(rowsAtClose).containsExactly(2L);
    }

    @Test
    public void metaDataValueProviderThrows() {
        final SQLException failure = new SQLException("no metadata");

        assertThatThrownBy(() -> handle.queryMetadata((MetaData.MetaDataValueProvider<String>) metaData -> {
            throw failure;
        })).isInstanceOf(UnableToRetrieveMetaDataException.class).hasCause(failure);

        assertThat(failuresAtClose).containsExactly(failure);
    }

    @Test
    public void metaDataValueProviderThrowsRuntimeException() {
        assertThatThrownBy(() -> handle.queryMetadata((MetaData.MetaDataValueProvider<String>) metaData -> {
            throw boom;
        })).isSameAs(boom);

        assertThat(failuresAtClose).containsExactly(boom);
    }

    @Test
    public void resultProducerThrows() {
        try (Update update = handle.createUpdate("update something set name = 'Apple' where id = 1")) {
            assertThatThrownBy(() -> update.execute((statementSupplier, ctx) -> {
                statementSupplier.get();
                throw boom;
            })).isSameAs(boom);
        }

        assertThat(failuresAtClose).containsExactly(boom);
    }

    @Test
    public void resultProducerThrowsCheckedException() {
        try (Update update = handle.createUpdate("update something set name = 'Apple' where id = 1")) {
            assertThatThrownBy(() -> update.execute((statementSupplier, ctx) -> {
                statementSupplier.get();
                throw Sneaky.throwAnyway(checkedBoom);
            })).isSameAs(checkedBoom);
        }

        assertThat(failuresAtClose).containsExactly(checkedBoom);
    }

    @Test
    public void queryResultProducerThrowsCheckedException() {
        try (Query query = handle.createQuery(SELECT)) {
            assertThatThrownBy(() -> query.execute((statementSupplier, ctx) -> {
                statementSupplier.get();
                throw Sneaky.throwAnyway(checkedBoom);
            })).isSameAs(checkedBoom);
        }

        assertThat(failuresAtClose).containsExactly(checkedBoom);
    }

    @Test
    public void preparedBatchResultProducerThrowsCheckedException() {
        try (PreparedBatch batch = handle.prepareBatch("update something set name = :name where id = 1")) {
            batch.bind("name", "Apple").add();
            assertThatThrownBy(() -> batch.execute((statementSupplier, ctx) -> {
                statementSupplier.get();
                throw Sneaky.throwAnyway(checkedBoom);
            })).isSameAs(checkedBoom);
        }

        assertThat(failuresAtClose).containsExactly(checkedBoom);
    }

    @Test
    public void batchTemplateThrowsCheckedException() {
        assertThatThrownBy(() -> handle.createBatch()
            .setTemplateEngine((template, ctx) -> {
                throw Sneaky.throwAnyway(checkedBoom);
            })
            .add("update something set name = 'Apple' where id = 1")
            .execute()).isSameAs(checkedBoom);

        assertThat(failuresAtClose).containsExactly(checkedBoom);
    }

    @Test
    public void firstFailureWins() {
        final StatementContext ctx = StatementContextAccess.createContext();
        final SQLException first = new SQLException("first");
        ctx.recordFailure(first);
        ctx.recordFailure(boom);

        assertThat(ctx.getFailure()).isSameAs(first);
    }

    public record MissingColumn(int id, String missing) {}

    public static final class Unparseable {
        public Unparseable(final int id, final String name) throws ParseException {
            throw new ParseException(name, id);
        }
    }
}
