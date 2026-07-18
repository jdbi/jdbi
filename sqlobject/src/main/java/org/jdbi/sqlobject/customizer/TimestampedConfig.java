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
package org.jdbi.sqlobject.customizer;

import java.time.ZoneId;

import org.jdbi.core.config.JdbiConfig;

/**
 * Configuration for {@link Timestamped}.
 */
public final class TimestampedConfig implements JdbiConfig<TimestampedConfig> {
    private final ZoneId timezone;

    public TimestampedConfig() {
        this(ZoneId.systemDefault());
    }

    private TimestampedConfig(ZoneId timezone) {
        this.timezone = timezone;
    }

    /**
     * Returns the timezone used in the resulting {@link java.time.OffsetDateTime}.
     *
     * @return The timezone used in the resulting {@link java.time.OffsetDateTime}
     */
    public ZoneId getTimezone() {
        return timezone;
    }

    /**
     * Returns a copy of this configuration with the given timezone used for the conversion of
     * {@link java.time.OffsetDateTime} objects.
     *
     * @param timezone used in the resulting {@link java.time.OffsetDateTime}
     * @return the derived configuration
     */
    public TimestampedConfig timezone(ZoneId timezone) {
        return new TimestampedConfig(timezone);
    }

    @Override
    public TimestampedConfig createCopy() {
        // Immutable: safe to share across registries.
        return this;
    }
}
