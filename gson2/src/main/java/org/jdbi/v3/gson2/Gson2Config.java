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
package org.jdbi.v3.gson2;

import com.google.gson.Gson;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.meta.Beta;

/**
 * Configuration class for Gson 2 integration.
 */
@Beta
public class Gson2Config implements JdbiConfig<Gson2Config> {
    private Gson gson;

    public Gson2Config() {
        this.gson = new Gson();
    }

    private Gson2Config(Gson2Config other) {
        this.gson = other.gson;
    }

    /**
     * Set the {@link Gson} to use for json conversion.
     * @param gson the mapper to use
     * @return this
     */
    public Gson2Config setGson(Gson gson) {
        this.gson = gson;
        return this;
    }

    /**
     * @return the {@link Gson} to use for json conversion
     */
    public Gson getGson() {
        return gson;
    }

    @Override
    public Gson2Config createCopy() {
        return new Gson2Config(this);
    }
}
