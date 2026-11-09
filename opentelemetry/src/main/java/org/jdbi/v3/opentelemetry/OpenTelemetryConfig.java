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

import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.meta.Alpha;

/**
 * Configuration for the spans of the {@link JdbiOpenTelemetryPlugin}.
 */
@Alpha
public final class OpenTelemetryConfig implements JdbiConfig<OpenTelemetryConfig> {
    private volatile boolean includeReturnedRows;

    public OpenTelemetryConfig() {}

    private OpenTelemetryConfig(final OpenTelemetryConfig that) {
        this.includeReturnedRows = that.includeReturnedRows;
    }

    /**
     * Whether spans record {@code db.response.returned_rows}.
     *
     * @return true if spans record the number of returned rows
     * @see #setIncludeReturnedRows(boolean)
     */
    public boolean getIncludeReturnedRows() {
        return includeReturnedRows;
    }

    /**
     * Set whether spans record {@code db.response.returned_rows}, the number of rows that a query, or a statement that
     * returns generated keys, mapped. The default is false, because the semantic conventions make this attribute opt-in.
     *
     * @param includeReturnedRows true to record the number of returned rows
     * @return this
     */
    public OpenTelemetryConfig setIncludeReturnedRows(final boolean includeReturnedRows) {
        this.includeReturnedRows = includeReturnedRows;
        return this;
    }

    @Override
    public OpenTelemetryConfig createCopy() {
        return new OpenTelemetryConfig(this);
    }
}
