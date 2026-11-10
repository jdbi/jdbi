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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.jdbi.v3.opentelemetry.JdbiOpenTelemetryPlugin.DB_QUERY_TEXT;
import static org.jdbi.v3.opentelemetry.JdbiOpenTelemetryPlugin.DB_SYSTEM_NAME;
import static org.jdbi.v3.opentelemetry.JdbiOpenTelemetryPlugin.ERROR_TYPE;
import static org.jdbi.v3.opentelemetry.JdbiOpenTelemetryPlugin.OTHER_SQL;

public class TestFailSafeTelemetry {
    private static final String SELECT = "select 1";

    @RegisterExtension
    JdbiExtension ext = JdbiExtension.h2();

    private final InMemorySpanExporter traces = new InMemorySpanExporter();
    private final AtomicInteger metaDataCalls = new AtomicInteger();

    @BeforeEach
    @AfterEach
    void resetGlobalOpenTelemetry() {
        GlobalOpenTelemetry.resetForTest();
    }

    @Test
    void nullMetaData() {
        final Jdbi jdbi = jdbi(connection -> null);
        jdbi.installPlugin(new JdbiOpenTelemetryPlugin(sdk(SimpleSpanProcessor.create(traces), Sampler.alwaysOn())));

        assertThat(selectTwice(jdbi)).containsExactly(1, 1);
        assertThat(metaDataCalls).hasValue(2);
        assertThat(traces.getExported())
            .extracting(span -> span.getAttributes().get(DB_SYSTEM_NAME))
            .containsExactly(OTHER_SQL, OTHER_SQL);
    }

    @Test
    void nullProductName() {
        final Jdbi jdbi = jdbi(connection -> (DatabaseMetaData) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] {DatabaseMetaData.class}, (proxy, method, args) -> null));
        jdbi.installPlugin(new JdbiOpenTelemetryPlugin(sdk(SimpleSpanProcessor.create(traces), Sampler.alwaysOn())));

        assertThat(selectTwice(jdbi)).containsExactly(1, 1);
        assertThat(metaDataCalls).hasValue(2);
        assertThat(traces.getExported())
            .extracting(span -> span.getAttributes().get(DB_SYSTEM_NAME))
            .containsExactly(OTHER_SQL, OTHER_SQL);
    }

    @Test
    void metaDataThrows() {
        final Jdbi jdbi = jdbi(connection -> {
            throw new IllegalStateException("no metadata");
        });
        jdbi.installPlugin(new JdbiOpenTelemetryPlugin(sdk(SimpleSpanProcessor.create(traces), Sampler.alwaysOn())));

        assertThat(selectTwice(jdbi)).containsExactly(1, 1);
        assertThat(metaDataCalls).hasValue(2);
        assertThat(traces.getExported())
            .extracting(span -> span.getAttributes().get(DB_SYSTEM_NAME))
            .containsExactly(OTHER_SQL, OTHER_SQL);
    }

    @Test
    void metaDataReadAgainAfterFailure() {
        final Jdbi jdbi = jdbi(connection -> {
            if (metaDataCalls.get() == 1) {
                throw new SQLException("no metadata yet");
            }
            return connection.getMetaData();
        });
        jdbi.installPlugin(new JdbiOpenTelemetryPlugin(sdk(SimpleSpanProcessor.create(traces), Sampler.alwaysOn())));

        assertThat(selectTwice(jdbi)).containsExactly(1, 1);
        assertThat(selectTwice(jdbi)).containsExactly(1, 1);
        assertThat(metaDataCalls).hasValue(2);
        assertThat(traces.getExported())
            .extracting(span -> span.getAttributes().get(DB_SYSTEM_NAME))
            .containsExactly(OTHER_SQL, "h2database", "h2database", "h2database");
    }

    @Test
    void globalOpenTelemetryWithoutSdk() {
        final Jdbi jdbi = jdbi(connection -> {
            throw new IllegalStateException("no metadata");
        });
        jdbi.installPlugin(new JdbiOpenTelemetryPlugin());

        assertThat(selectTwice(jdbi)).containsExactly(1, 1);
        assertThat(metaDataCalls).hasValue(0);
    }

    @Test
    void noopOpenTelemetry() {
        final Jdbi jdbi = jdbi(connection -> null);
        jdbi.installPlugin(new JdbiOpenTelemetryPlugin(OpenTelemetry.noop()));

        assertThat(selectTwice(jdbi)).containsExactly(1, 1);
        assertThat(metaDataCalls).hasValue(0);
    }

    @Test
    void samplerThrows() {
        assertSamplerFailureIgnored(new IllegalStateException("sampler failed"));
    }

    @Test
    void samplerThrowsCheckedException() {
        assertSamplerFailureIgnored(new IOException("sampler failed"));
    }

    private void assertSamplerFailureIgnored(final Exception failure) {
        final Sampler sampler = new Sampler() {
            @Override
            public SamplingResult shouldSample(final Context parentContext, final String traceId, final String name,
                final SpanKind spanKind, final Attributes attributes, final List<LinkData> parentLinks) {
                throw sneakyThrow(failure);
            }

            @Override
            public String getDescription() {
                return "throwing sampler";
            }
        };
        ext.getJdbi().installPlugin(new JdbiOpenTelemetryPlugin(sdk(SimpleSpanProcessor.create(traces), sampler)));

        assertThat(selectTwice(ext.getJdbi())).containsExactly(1, 1);
        assertThat(traces.getExported()).isEmpty();
    }

    @Test
    void spanEndThrows() {
        assertSpanEndFailureIgnored(new IllegalStateException("span processor failed"));
    }

    @Test
    void spanEndThrowsCheckedException() {
        assertSpanEndFailureIgnored(new IOException("span processor failed"));
    }

    private void assertSpanEndFailureIgnored(final Exception failure) {
        final SpanProcessor processor = new SpanProcessor() {
            @Override
            public void onStart(final Context parentContext, final ReadWriteSpan span) {}

            @Override
            public boolean isStartRequired() {
                return false;
            }

            @Override
            public void onEnd(final ReadableSpan span) {
                throw sneakyThrow(failure);
            }

            @Override
            public boolean isEndRequired() {
                return true;
            }
        };
        ext.getJdbi().installPlugin(new JdbiOpenTelemetryPlugin(sdk(processor, Sampler.alwaysOn())));

        assertThat(selectTwice(ext.getJdbi())).containsExactly(1, 1);
    }

    @Test
    void attributeFailureEndsSpan() {
        ext.getJdbi().getConfig(SqlStatements.class).setJfrSqlMaxLength(-1);
        ext.getJdbi().installPlugin(new JdbiOpenTelemetryPlugin(sdk(SimpleSpanProcessor.create(traces), Sampler.alwaysOn())));

        assertThat(selectTwice(ext.getJdbi())).containsExactly(1, 1);
        assertThat(traces.getExported())
            .extracting(span -> span.getAttributes().get(DB_SYSTEM_NAME), span -> span.getAttributes().get(DB_QUERY_TEXT))
            .containsExactly(
                tuple("h2database", null),
                tuple("h2database", null));
    }

    @Test
    void attributeThrowsCheckedException() {
        final List<AttributeFailingSpan> spans = new CopyOnWriteArrayList<>();
        final Tracer tracer = name -> (SpanBuilder) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] {SpanBuilder.class}, (proxy, method, args) -> {
                if ("startSpan".equals(method.getName())) {
                    final AttributeFailingSpan span = new AttributeFailingSpan();
                    spans.add(span);
                    return span;
                }
                return proxy;
            });
        ext.getJdbi().installPlugin(new JdbiOpenTelemetryPlugin(openTelemetry(tracer)));

        try (Handle handle = ext.getJdbi().open()) {
            assertThat(handle.createQuery(SELECT).mapTo(Integer.class).one()).isOne();
        }

        assertThat(spans).singleElement().satisfies(span -> {
            assertThat(span.attributeCalls).hasValue(1);
            assertThat(span.ends).hasValue(1);
        });
    }

    @Test
    void sampledOutSpanDescribesNoArguments() {
        ext.getJdbi().getConfig(SqlStatements.class).setIncludeBindingsInTelemetry(true);
        ext.getJdbi().installPlugin(new JdbiOpenTelemetryPlugin(sdk(SimpleSpanProcessor.create(traces), Sampler.alwaysOff())));
        final CountingBean bean = new CountingBean();

        try (Handle handle = ext.getJdbi().open()) {
            assertThat(handle.createQuery("select cast(:name as varchar)").bindBean(bean).mapTo(String.class).one())
                .isEqualTo("Alice");
        }

        assertThat(bean.reads).hasValue(1);
        assertThat(traces.getExported()).isEmpty();
    }

    @Test
    void describedBeanThrowsCheckedException() {
        final CountingSpanProcessor counts = installCountingPlugin();
        final FailingBean bean = new FailingBean(2);

        try (Handle handle = ext.getJdbi().open()) {
            handle.execute("create table something (name varchar(50))");
            assertThat(handle.createUpdate("insert into something (name) values (:name)").bindBean(bean).execute()).isOne();
            assertThat(handle.createQuery("select name from something").mapTo(String.class).list()).containsExactly("Alice");
        }

        assertThat(bean.reads).hasValue(2);
        assertThat(counts.ended).hasValue(counts.started.get());
        assertThat(traces.getExported())
            .extracting(span -> span.getStatus().getStatusCode())
            .containsExactly(StatusCode.UNSET, StatusCode.UNSET, StatusCode.UNSET);
    }

    @Test
    void boundBeanThrowsCheckedException() {
        final CountingSpanProcessor counts = installCountingPlugin();
        final FailingBean bean = new FailingBean(1);

        try (Handle handle = ext.getJdbi().open()) {
            handle.execute("create table something (name varchar(50))");
            assertThatThrownBy(() -> handle.createUpdate("insert into something (name) values (:name)").bindBean(bean).execute())
                .isInstanceOf(GeneralSecurityException.class);
        }

        assertThat(counts.ended).hasValue(counts.started.get());
        final SpanData failed = traces.getExported().get(1);
        assertThat(failed.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
        assertThat(failed.getAttributes().get(ERROR_TYPE)).isEqualTo(GeneralSecurityException.class.getName());
    }

    private static OpenTelemetry openTelemetry(final Tracer tracer) {
        final TracerProvider tracerProvider = new TracerProvider() {
            @Override
            public Tracer get(final String instrumentationScopeName) {
                return tracer;
            }

            @Override
            public Tracer get(final String instrumentationScopeName, final String instrumentationScopeVersion) {
                return tracer;
            }
        };
        return new OpenTelemetry() {
            @Override
            public TracerProvider getTracerProvider() {
                return tracerProvider;
            }

            @Override
            public ContextPropagators getPropagators() {
                return ContextPropagators.noop();
            }
        };
    }

    private static final class AttributeFailingSpan implements Span {
        private final SpanContext spanContext = SpanContext.create("0af7651916cd43dd8448eb211c80319c", "b7ad6b7169203331",
            TraceFlags.getSampled(), TraceState.getDefault());
        private final AtomicInteger attributeCalls = new AtomicInteger();
        private final AtomicInteger ends = new AtomicInteger();

        @Override
        public <T> Span setAttribute(final AttributeKey<T> key, final T value) {
            attributeCalls.incrementAndGet();
            throw sneakyThrow(new IOException("attribute failed"));
        }

        @Override
        public Span addEvent(final String name, final Attributes attributes) {
            return this;
        }

        @Override
        public Span addEvent(final String name, final Attributes attributes, final long timestamp, final TimeUnit unit) {
            return this;
        }

        @Override
        public Span setStatus(final StatusCode statusCode, final String description) {
            return this;
        }

        @Override
        public Span recordException(final Throwable exception, final Attributes additionalAttributes) {
            return this;
        }

        @Override
        public Span updateName(final String name) {
            return this;
        }

        @Override
        public void end() {
            ends.incrementAndGet();
        }

        @Override
        public void end(final long timestamp, final TimeUnit unit) {
            ends.incrementAndGet();
        }

        @Override
        public SpanContext getSpanContext() {
            return spanContext;
        }

        @Override
        public boolean isRecording() {
            return true;
        }
    }

    private CountingSpanProcessor installCountingPlugin() {
        final CountingSpanProcessor counts = new CountingSpanProcessor();
        ext.getJdbi().getConfig(SqlStatements.class).setIncludeBindingsInTelemetry(true);
        ext.getJdbi().installPlugin(new JdbiOpenTelemetryPlugin(
            sdk(SpanProcessor.composite(counts, SimpleSpanProcessor.create(traces)), Sampler.alwaysOn())));
        return counts;
    }

    private static final class CountingSpanProcessor implements SpanProcessor {
        private final AtomicInteger started = new AtomicInteger();
        private final AtomicInteger ended = new AtomicInteger();

        @Override
        public void onStart(final Context parentContext, final ReadWriteSpan span) {
            started.incrementAndGet();
        }

        @Override
        public boolean isStartRequired() {
            return true;
        }

        @Override
        public void onEnd(final ReadableSpan span) {
            ended.incrementAndGet();
        }

        @Override
        public boolean isEndRequired() {
            return true;
        }
    }

    public static final class FailingBean {
        private final AtomicInteger reads = new AtomicInteger();
        private final int failingRead;

        FailingBean(final int failingRead) {
            this.failingRead = failingRead;
        }

        public String getName() throws Exception {
            if (reads.incrementAndGet() == failingRead) {
                throw new GeneralSecurityException("unreadable");
            }
            return "Alice";
        }
    }

    public static final class CountingBean {
        private final AtomicInteger reads = new AtomicInteger();

        public String getName() {
            reads.incrementAndGet();
            return "Alice";
        }
    }

    private static List<Integer> selectTwice(final Jdbi jdbi) {
        try (Handle handle = jdbi.open()) {
            return List.of(
                handle.createQuery(SELECT).mapTo(Integer.class).one(),
                handle.createQuery(SELECT).mapTo(Integer.class).one());
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Exception> RuntimeException sneakyThrow(final Exception e) throws E {
        throw (E) e;
    }

    private static OpenTelemetry sdk(final SpanProcessor processor, final Sampler sampler) {
        return OpenTelemetrySdk.builder()
            .setTracerProvider(SdkTracerProvider.builder()
                .addSpanProcessor(processor)
                .setSampler(sampler)
                .build())
            .build();
    }

    private Jdbi jdbi(final MetaDataSource metaData) {
        return Jdbi.create(() -> {
            final Connection connection = DriverManager.getConnection(ext.getUrl());
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Connection.class},
                (proxy, method, args) -> {
                    if ("getMetaData".equals(method.getName())) {
                        metaDataCalls.incrementAndGet();
                        return metaData.get(connection);
                    }
                    return invoke(connection, method, args);
                });
        });
    }

    @FunctionalInterface
    private interface MetaDataSource {
        DatabaseMetaData get(Connection connection) throws SQLException;
    }

    private static Object invoke(final Connection connection, final Method method, final Object[] args) throws Throwable {
        try {
            return method.invoke(connection, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
