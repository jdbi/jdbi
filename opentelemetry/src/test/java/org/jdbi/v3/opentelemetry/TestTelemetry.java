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
package org.jdbi.v3.opentelemetry;

import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import jdk.jfr.consumer.RecordedEvent;
import org.assertj.core.api.Assertions;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.statement.JdbiStatementEvent;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.moditect.jfrunit.EnableEvent;
import org.moditect.jfrunit.JfrEventTest;
import org.moditect.jfrunit.JfrEvents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.jdbi.v3.opentelemetry.JdbiOpenTelemetryPlugin.DB_QUERY_PARAMETER_PREFIX;
import static org.jdbi.v3.opentelemetry.JdbiOpenTelemetryPlugin.DB_QUERY_SUMMARY;
import static org.jdbi.v3.opentelemetry.JdbiOpenTelemetryPlugin.DB_QUERY_TEXT;
import static org.jdbi.v3.opentelemetry.JdbiOpenTelemetryPlugin.DB_RESPONSE_RETURNED_ROWS;
import static org.jdbi.v3.opentelemetry.JdbiOpenTelemetryPlugin.DB_RESPONSE_STATUS_CODE;
import static org.jdbi.v3.opentelemetry.JdbiOpenTelemetryPlugin.DB_SYSTEM_NAME;
import static org.jdbi.v3.opentelemetry.JdbiOpenTelemetryPlugin.ERROR_TYPE;

@JfrEventTest
@EnableEvent(JdbiStatementEvent.NAME)
@EnabledIf("org.jdbi.v3.core.statement.internal.JfrSupport#isFlightRecorderAvailable")
// While JFR is available all the way back to Java 9, the utilities to test it are not.
public class TestTelemetry {
    private static final String CREATE = "create table something(id identity primary key, name varchar(50))";
    private static final String INSERT = "insert into something (id, name) values (:id, :name)";
    private static final String SELECT = "select * from something";

    @RegisterExtension
    JdbiExtension ext = JdbiExtension.h2();

    public JfrEvents jfrEvents = new JfrEvents();

    InMemorySpanExporter traces = new InMemorySpanExporter();

    Class<?> testCode;
    Object instance;

    public TestTelemetry() throws ReflectiveOperationException {
        this.testCode = Class.forName(this.getClass().getName() + "$TestCode");
        this.instance = testCode.getDeclaredConstructors()[0].newInstance(this);
    }

    private void run(final String testName) throws ReflectiveOperationException {
        try {
            testCode.getMethod(testName).invoke(instance);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Error error) {
                throw error;
            }
            throw e;
        }
    }

    @BeforeEach
    void setupOpenTelemetry() throws ReflectiveOperationException {
        run("setupOpenTelemetry");
    }

    @AfterEach
    void resetOpenTelemetry() throws ReflectiveOperationException {
        run("resetOpenTelemetry");
    }

    @Test
    void events() throws ReflectiveOperationException {
        run("events");
    }

    @Test
    void includeBindings() throws ReflectiveOperationException {
        run("includeBindings");
    }

    @Test
    void positionalBindings() throws ReflectiveOperationException {
        run("positionalBindings");
    }

    @Test
    void noBindingsOnBatch() throws ReflectiveOperationException {
        run("noBindingsOnBatch");
    }

    @Test
    void truncate() throws ReflectiveOperationException {
        run("truncate");
    }

    @Test
    void bindingsExcludedByDefault() throws ReflectiveOperationException {
        run("bindingsExcludedByDefault");
    }

    @Test
    void failure() throws ReflectiveOperationException {
        run("failure");
    }

    @Test
    void failureReplacedByLaterHandler() throws ReflectiveOperationException {
        run("failureReplacedByLaterHandler");
    }

    @Test
    void failureBeforeExecution() throws ReflectiveOperationException {
        run("failureBeforeExecution");
    }

    @Test
    void unprintableArgument() throws ReflectiveOperationException {
        run("unprintableArgument");
    }

    @Test
    void templateFailure() throws ReflectiveOperationException {
        run("templateFailure");
    }

    @Test
    void batchFailure() throws ReflectiveOperationException {
        run("batchFailure");
    }

    @Test
    void preparedBatchFailure() throws ReflectiveOperationException {
        run("preparedBatchFailure");
    }

    @Test
    void statementSubclass() throws ReflectiveOperationException {
        run("statementSubclass");
    }

    @Test
    void sqlObjectSummary() throws ReflectiveOperationException {
        run("sqlObjectSummary");
    }

    public interface SomethingDao {
        @SqlQuery("select name from something where id = :id")
        String findName(@Bind("id") int id);
    }

    public final class TestCode {
        OpenTelemetrySdk otelSdk;

        public void setupOpenTelemetry() {
            final var tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(traces))
                .setSampler(Sampler.alwaysOn())
                .build();
            otelSdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();

            ext.getJdbi().installPlugin(new JdbiOpenTelemetryPlugin(otelSdk));
        }

        public void resetOpenTelemetry() {
            traces.clearExported();
        }

        public void events() {
            final var span = otelSdk.getTracer("test")
                .spanBuilder("testSpan")
                .startSpan();
            Assertions.assertThat(span.getSpanContext().isValid()).isTrue();

            final var traceId = span.getSpanContext().getTraceId();

            try (var scope = span.makeCurrent(); var h = ext.openHandle()) {
                h.execute(CREATE);
                insertSomething(h, 1, "Zebra");
                insertSomething(h, 2, "Bananas");
                Assertions.assertThat(h.createQuery(SELECT)
                    .mapToMap()
                    .list())
                    .hasSize(2);
            } finally {
                span.end();
            }

            jfrEvents.awaitEvents();
            final var events = extractEventProperties();
            assertThat(events)
                .extracting(e -> e.get("type"), e -> e.get("sql"), e -> e.get("parameters"), e -> e.get("rowsMapped"), e -> e.get("traceId"))
                .containsExactly(
                    tuple("Update", CREATE, null, 0L, traceId),
                    tuple("Update", INSERT, null, 0L, traceId),
                    tuple("Update", INSERT, null, 0L, traceId),
                    tuple("Query", SELECT, null, 2L, traceId));

            final var spans = traces.getExported();
            assertThat(spans)
                .extracting(
                    SpanData::getTraceId,
                    SpanData::getName,
                    SpanData::getKind,
                    sd -> sd.getAttributes().get(DB_SYSTEM_NAME),
                    sd -> sd.getAttributes().get(DB_QUERY_TEXT),
                    sd -> sd.getAttributes().get(DB_RESPONSE_RETURNED_ROWS))
                .containsExactly(
                    tuple(traceId, "testSpan", SpanKind.INTERNAL, null, null, null),
                    tuple(traceId, "h2database", SpanKind.CLIENT, "h2database", CREATE, null),
                    tuple(traceId, "h2database", SpanKind.CLIENT, "h2database", INSERT, null),
                    tuple(traceId, "h2database", SpanKind.CLIENT, "h2database", INSERT, null),
                    tuple(traceId, "h2database", SpanKind.CLIENT, "h2database", SELECT, 2L));
            assertThat(spans)
                .allSatisfy(sd -> {
                    assertThat(sd.getStatus().getStatusCode()).isEqualTo(StatusCode.UNSET);
                    assertThat(sd.getAttributes().get(DB_QUERY_SUMMARY)).isNull();
                    assertThat(parameters(sd)).isEmpty();
                });
        }

        public void includeBindings() {
            ext.getJdbi().getConfig(SqlStatements.class).setIncludeBindingsInTelemetry(true);

            try (var h = ext.openHandle()) {
                h.execute(CREATE);
                insertSomething(h, 1, "Zebra");
            }

            jfrEvents.awaitEvents();
            assertThat(extractEventProperties())
                .extracting(e -> e.get("parameters"))
                .containsExactly("{}", "{named:{id:1,name:Zebra}}");

            assertThat(traces.getExported())
                .extracting(this::parameters)
                .containsExactly(
                    Map.of(),
                    Map.of(DB_QUERY_PARAMETER_PREFIX + "id", "1", DB_QUERY_PARAMETER_PREFIX + "name", "Zebra"));
        }

        public void positionalBindings() {
            ext.getJdbi().getConfig(SqlStatements.class).setIncludeBindingsInTelemetry(true);

            try (var h = ext.openHandle()) {
                h.execute(CREATE);
                h.createUpdate("insert into something (id, name) values (?, ?)")
                    .bind(0, 1)
                    .bind(1, "Zebra")
                    .execute();
            }

            assertThat(parameters(traces.getExported().get(1)))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                    DB_QUERY_PARAMETER_PREFIX + "0", "1",
                    DB_QUERY_PARAMETER_PREFIX + "1", "Zebra"));
        }

        public void noBindingsOnBatch() {
            ext.getJdbi().getConfig(SqlStatements.class).setIncludeBindingsInTelemetry(true);

            try (var h = ext.openHandle()) {
                h.execute(CREATE);
                h.prepareBatch(INSERT)
                    .bind("id", 1).bind("name", "Zebra").add()
                    .bind("id", 2).bind("name", "Bananas").add()
                    .execute();
            }

            final var spans = traces.getExported();
            assertThat(spans).hasSize(2);
            assertThat(spans.get(1).getAttributes().get(DB_QUERY_TEXT)).isEqualTo(INSERT);
            assertThat(spans).allSatisfy(sd -> assertThat(parameters(sd)).isEmpty());
        }

        public void truncate() {
            ext.getJdbi().getConfig(SqlStatements.class)
                .setIncludeBindingsInTelemetry(true)
                .setJfrSqlMaxLength(10)
                .setJfrParamMaxLength(12);
            try (var h = ext.openHandle()) {
                h.execute(CREATE);
                insertSomething(h, 1, "abcdefghijklmnopqrstuvwxyz");
            }

            jfrEvents.awaitEvents();
            assertThat(extractEventProperties())
                .extracting(e -> e.get("sql"), e -> e.get("parameters"))
                .containsExactly(
                    tuple(CREATE.substring(0, 10), "{}"),
                    tuple(INSERT.substring(0, 10), "{named:{id…}"));

            final var spans = traces.getExported();
            assertThat(spans)
                .extracting(sd -> sd.getAttributes().get(DB_QUERY_TEXT))
                .containsExactly(CREATE.substring(0, 10), INSERT.substring(0, 10));
            assertThat(parameters(spans.get(1)))
                .containsEntry(DB_QUERY_PARAMETER_PREFIX + "name", "abcdefghijk…");
        }

        public void bindingsExcludedByDefault() {
            final var sensitiveData = "sensitive_data";

            try (var h = ext.openHandle()) {
                h.execute(CREATE);
                insertSomething(h, 1, sensitiveData);
            }

            jfrEvents.awaitEvents();
            assertThat(extractEventProperties())
                .allSatisfy(e -> assertThat(e.values())
                    .noneSatisfy(value -> assertThat(String.valueOf(value)).contains(sensitiveData)));

            assertThat(traces.getExported())
                .allSatisfy(span -> span.getAttributes().forEach((key, value) ->
                    assertThat(String.valueOf(value)).doesNotContain(sensitiveData)));
        }

        public void failure() {
            try (var h = ext.openHandle()) {
                h.execute(CREATE);
                insertSomething(h, 1, "Zebra");
                assertThatThrownBy(() -> insertSomething(h, 1, "Duplicate"))
                    .isInstanceOf(UnableToExecuteStatementException.class);
            }

            final var spans = traces.getExported();
            assertThat(spans).hasSize(3);
            assertThat(spans.subList(0, 2))
                .allSatisfy(sd -> {
                    assertThat(sd.getStatus().getStatusCode()).isEqualTo(StatusCode.UNSET);
                    assertThat(sd.getAttributes().get(ERROR_TYPE)).isNull();
                });
            assertDuplicateKeyFailure(spans.get(2));
        }

        public void failureReplacedByLaterHandler() {
            ext.getJdbi().getConfig(SqlStatements.class).addExceptionHandler((ex, ctx) -> {
                throw new IllegalStateException("duplicate");
            });

            try (var h = ext.openHandle()) {
                h.execute(CREATE);
                insertSomething(h, 1, "Zebra");
                assertThatThrownBy(() -> insertSomething(h, 1, "Duplicate"))
                    .isInstanceOf(IllegalStateException.class);
            }

            assertDuplicateKeyFailure(traces.getExported().get(2));
        }

        public void failureBeforeExecution() {
            try (var h = ext.openHandle()) {
                assertThatThrownBy(() -> h.createQuery("select name from missing_table").mapToMap().list())
                    .isInstanceOf(UnableToCreateStatementException.class);
            }

            final var failed = traces.getExported().get(0);
            assertThat(failed.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
            assertThat(failed.getAttributes().get(DB_RESPONSE_STATUS_CODE)).isEqualTo("42S04");
            assertThat(failed.getAttributes().get(ERROR_TYPE)).isEqualTo("42S04");
        }

        public void unprintableArgument() {
            ext.getJdbi().getConfig(SqlStatements.class).setIncludeBindingsInTelemetry(true);
            final Argument unprintable = new Argument() {
                @Override
                public void apply(final int position, final PreparedStatement statement, final StatementContext ctx) throws SQLException {
                    statement.setString(position, "Zebra");
                }

                @Override
                public String toString() {
                    throw new IllegalStateException("no toString");
                }
            };

            final String description = "<" + unprintable.getClass().getName() + ".toString() threw java.lang.IllegalStateException>";

            try (var h = ext.openHandle()) {
                h.execute(CREATE);
                assertThat(h.createUpdate(INSERT).bind("id", 1).bind("name", unprintable).execute()).isEqualTo(1);
            }

            jfrEvents.awaitEvents();
            assertThat(extractEventProperties().get(1).get("parameters")).isEqualTo("{named:{id:1,name:" + description + "}}");

            final var insert = traces.getExported().get(1);
            assertThat(insert.getStatus().getStatusCode()).isEqualTo(StatusCode.UNSET);
            assertThat(parameters(insert)).containsEntry(DB_QUERY_PARAMETER_PREFIX + "name", description);
        }

        public void templateFailure() {
            try (var h = ext.openHandle()) {
                assertThatThrownBy(() -> h.createQuery("select <missing> from something").mapToMap().list())
                    .isInstanceOf(UnableToCreateStatementException.class);

                final var failed = traces.getExported();
                assertThat(failed).hasSize(1);
                assertThat(failed.get(0).getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
                assertThat(failed.get(0).getAttributes().get(ERROR_TYPE))
                    .isEqualTo(UnableToCreateStatementException.class.getName());
                assertThat(failed.get(0).getAttributes().get(DB_RESPONSE_STATUS_CODE)).isNull();
            }
        }

        public void batchFailure() {
            try (var h = ext.openHandle()) {
                h.execute(CREATE);
                assertThatThrownBy(() -> h.createBatch()
                    .add("insert into something (id, name) values (1, 'Zebra')")
                    .add("insert into something (id, name) values (1, 'Duplicate')")
                    .execute())
                    .isInstanceOf(UnableToExecuteStatementException.class);
            }

            assertDuplicateKeyFailure(traces.getExported().get(1));
        }

        public void preparedBatchFailure() {
            try (var h = ext.openHandle()) {
                h.execute(CREATE);
                assertThatThrownBy(() -> h.prepareBatch(INSERT)
                    .bind("id", 1).bind("name", "Zebra").add()
                    .bind("id", 1).bind("name", "Duplicate").add()
                    .execute())
                    .isInstanceOf(UnableToExecuteStatementException.class);
            }

            assertDuplicateKeyFailure(traces.getExported().get(1));
        }

        public void statementSubclass() {
            try (var h = ext.openHandle()) {
                h.execute(CREATE);
                insertSomething(h, 1, "Zebra");
                assertThat(new Query(h, SELECT) {}.mapToMap().list()).hasSize(1);
            }

            assertThat(traces.getExported().get(2).getAttributes().get(DB_RESPONSE_RETURNED_ROWS)).isEqualTo(1L);
        }

        private void assertDuplicateKeyFailure(final SpanData failed) {
            assertThat(failed.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
            assertThat(failed.getStatus().getDescription()).isEmpty();
            assertThat(failed.getAttributes().get(DB_RESPONSE_STATUS_CODE)).isEqualTo("23505");
            assertThat(failed.getAttributes().get(ERROR_TYPE)).isEqualTo("23505");
            assertThat(failed.getEvents()).isEmpty();
        }

        public void sqlObjectSummary() {
            ext.getJdbi().installPlugin(new SqlObjectPlugin());

            try (var h = ext.openHandle()) {
                h.execute(CREATE);
                insertSomething(h, 1, "Zebra");
                assertThat(h.attach(SomethingDao.class).findName(1)).isEqualTo("Zebra");
            }

            final var query = traces.getExported().get(2);
            assertThat(query.getName()).isEqualTo("SomethingDao.findName");
            assertThat(query.getAttributes().get(DB_QUERY_SUMMARY)).isEqualTo("SomethingDao.findName");
            assertThat(query.getAttributes().get(DB_RESPONSE_RETURNED_ROWS)).isEqualTo(1L);
        }

        private Map<String, Object> parameters(final SpanData span) {
            return span.getAttributes().asMap().entrySet().stream()
                .filter(e -> e.getKey().getKey().startsWith(DB_QUERY_PARAMETER_PREFIX))
                .collect(Collectors.toMap(e -> e.getKey().getKey(), Map.Entry::getValue));
        }

        private List<Map<String, Object>> extractEventProperties() {
            return jfrEvents.events()
                .sorted(Comparator.comparing(RecordedEvent::getStartTime))
                .map(evt -> {
                    final var result = new TreeMap<String, Object>();
                    for (final var field : evt.getFields()) {
                        result.put(field.getName(), evt.getValue(field.getName()));
                    }
                    return result;
                })
                .collect(Collectors.toList());
        }

        private void insertSomething(final Handle h, final int id, final String name) {
            h.createUpdate(INSERT)
                .bind("id", id)
                .bind("name", name)
                .execute();
        }
    }
}
