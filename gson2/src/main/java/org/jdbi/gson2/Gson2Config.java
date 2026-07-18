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
package org.jdbi.gson2;

import com.google.gson.Gson;
import org.jdbi.core.config.JdbiConfig;

/**
 * Configuration class for Gson 2 integration.
 */
public final class Gson2Config implements JdbiConfig<Gson2Config> {
    private final Gson gson;

    public Gson2Config() {
        this(new Gson());
    }

    private Gson2Config(Gson gson) {
        this.gson = gson;
    }

    /**
     * Returns a copy of this configuration using the given {@link Gson} for json conversion.
     * @param gson the mapper to use
     * @return the derived configuration
     */
    public Gson2Config gson(Gson gson) {
        return new Gson2Config(gson);
    }

    /**
     * Returns the {@link Gson} object used for json conversion.
     *
     * @return the {@link Gson} object used for json conversion.
     */
    public Gson getGson() {
        return gson;
    }

    @Override
    public Gson2Config createCopy() {
        // Immutable: safe to share across registries.
        return this;
    }
}
