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
package org.jdbi.v3.java21.telemetry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import jdk.jfr.consumer.RecordedEvent;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.JdbiStatementEvent;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.opentelemetry.JdbiOpenTelemetryPlugin;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.moditect.jfrunit.EnableEvent;
import org.moditect.jfrunit.JfrEventTest;
import org.moditect.jfrunit.JfrEvents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@JfrEventTest
@EnableEvent(JdbiStatementEvent.NAME)
@EnabledIf("org.jdbi.v3.core.statement.internal.JfrSupport#isFlightRecorderAvailable")
// While JFR is available all the way back to Java 9, the utilities to test it are not.
public class TestTelemetry {
    @RegisterExtension
    JdbiExtension ext = JdbiExtension.h2();

    public JfrEvents jfrEvents = new JfrEvents();

    InMemorySpanExporter traces = new InMemorySpanExporter();

    Class<?> testCode;
    Method setupOpenTelemetryMethod;
    Method eventsMethod;
    Method truncateMethod;
    Object instance;

    public TestTelemetry() throws ReflectiveOperationException {
        this.testCode = Class.forName(this.getClass().getName() + "$TestCode");
        this.setupOpenTelemetryMethod = testCode.getMethod("setupOpenTelemetry");
        this.eventsMethod = testCode.getMethod("events");
        this.truncateMethod = testCode.getMethod("truncate");

        this.instance = testCode.getDeclaredConstructors()[0].newInstance(this);
    }

    @BeforeEach
    void setupOpenTelemetry() throws InvocationTargetException, IllegalAccessException {
        setupOpenTelemetryMethod.invoke(instance);
    }

    @Test
    void events() throws InvocationTargetException, IllegalAccessException {
        assertThat(eventsMethod.invoke(instance)).isNull();
    }

    @Test
    void truncate() throws InvocationTargetException, IllegalAccessException {
        assertThat(truncateMethod.invoke(instance)).isNull();
    }

    public final class TestCode {
        OpenTelemetrySdk otelSdk;


        @BeforeEach
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

        public void events() {
            final var span = otelSdk.getTracer("test")
                .spanBuilder("testSpan")
                .startSpan();
            assertThat(span.getSpanContext().isValid()).isTrue();

            final var traceId = span.getSpanContext().getTraceId();
            final var create = "create table something(id identity primary key, name varchar(50))";
            final var insert = "insert into something (id, name) values (:id, :name)";
            final var select = "select * from something";
            final var ins1Binding = "{named:{id:1,name:Zebra}}";
            final var ins2Binding = "{named:{id:2,name:Bananas}}";

            try (var scope = span.makeCurrent(); var h = ext.openHandle()) {
                h.execute(create);
                insertSomething(h, insert, 1, "Zebra");
                insertSomething(h, insert, 2, "Bananas");
                assertThat(h.createQuery(select)
                    .mapToMap()
                    .list())
                    .hasSize(2);
            } finally {
                span.end();
            }

            jfrEvents.awaitEvents();
            final var events = extractEventProperties();
            assertThat(events).hasSize(4);
            assertThat(events.get(0))
                .containsEntry("sql", create)
                .containsEntry("parameters", "{}")
                .containsEntry("rowsMapped", 0L)
                .containsEntry("type", "Update")
                .containsEntry("traceId", traceId);
            assertThat(events.get(1))
                .containsEntry("sql", insert)
                .containsEntry("parameters", ins1Binding)
                .containsEntry("rowsMapped", 0L)
                .containsEntry("type", "Update")
                .containsEntry("traceId", traceId);
            assertThat(events.get(2))
                .containsEntry("sql", insert)
                .containsEntry("parameters", ins2Binding)
                .containsEntry("rowsMapped", 0L)
                .containsEntry("type", "Update")
                .containsEntry("traceId", traceId);
            assertThat(events.get(3))
                .containsEntry("sql", select)
                .containsEntry("parameters", "{}")
                .containsEntry("rowsMapped", 2L)
                .containsEntry("type", "Query")
                .containsEntry("traceId", traceId);

            final var spans = traces.getExported();
            assertThat(spans).hasSize(5);
            assertThat(spans)
                .extracting(
                    SpanData::getTraceId,
                    SpanData::getName,
                    sd -> sd.getAttributes().get(JdbiOpenTelemetryPlugin.SQL),
                    sd -> sd.getAttributes().get(JdbiOpenTelemetryPlugin.BINDING))
                .containsExactly(
                    tuple(traceId, "testSpan", null, null),
                    tuple(traceId, "jdbi.Update", create, "{}"),
                    tuple(traceId, "jdbi.Update", insert, ins1Binding),
                    tuple(traceId, "jdbi.Update", insert, ins2Binding),
                    tuple(traceId, "jdbi.Query", select, "{}"));
        }

        public void truncate() {
            ext.getJdbi().getConfig(SqlStatements.class)
                .setJfrSqlMaxLength(10)
                .setJfrParamMaxLength(32);
            final var create = "create table something(id identity primary key, name varchar(50))";
            final var insert = "insert into something (id, name) values (:id, :name)";
            try (var h = ext.openHandle()) {
                h.execute(create);
                insertSomething(h, insert, 1, "abcdefghijklmnopqrstuvwxyz");
            }

            jfrEvents.awaitEvents();
            final var events = extractEventProperties();
            assertThat(events).hasSize(2);
            assertThat(events.get(0))
                .containsEntry("sql", create.substring(0, 10))
                .containsEntry("parameters", "{}")
                .containsEntry("type", "Update");
            assertThat(events.get(1))
                .containsEntry("sql", insert.substring(0, 10))
                .containsEntry("parameters", "{named:{id:1,name:abcdefghijkl…}")
                .containsEntry("type", "Update");
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

        private void insertSomething(final Handle h, final String sql, final int id, final String name) {
            h.createUpdate(sql)
                .bind("id", id)
                .bind("name", name)
                .execute();
        }
    }
}
