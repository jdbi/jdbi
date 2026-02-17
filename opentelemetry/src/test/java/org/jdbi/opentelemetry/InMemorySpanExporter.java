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
package org.jdbi.opentelemetry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import static io.opentelemetry.sdk.common.CompletableResultCode.ofFailure;
import static io.opentelemetry.sdk.common.CompletableResultCode.ofSuccess;

final class InMemorySpanExporter implements SpanExporter {
    private final List<SpanData> exported = new ArrayList<>();
    private boolean shutdown;

    @Override
    public synchronized CompletableResultCode export(final Collection<SpanData> spans) {
        if (shutdown) {
            return ofFailure();
        }
        exported.addAll(spans);
        return ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return ofSuccess();
    }

    @Override
    public synchronized CompletableResultCode shutdown() {
        shutdown = true;
        return ofSuccess();
    }

    public void clearExported() {
        flush();
        synchronized (this) {
            exported.clear();
        }
    }

    public List<SpanData> getExported() {
        flush();
        synchronized (this) {
            return exported.stream()
                    .sorted(Comparator.comparing(SpanData::getStartEpochNanos))
                    .toList();
        }
    }
}
