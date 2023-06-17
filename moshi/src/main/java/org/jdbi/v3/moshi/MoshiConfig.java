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
package org.jdbi.v3.moshi;

import com.squareup.moshi.Moshi;
import org.jdbi.v3.core.config.JdbiConfig;

/**
 * Configuration class for Moshi integration.
 */
public class MoshiConfig implements JdbiConfig<MoshiConfig> {

    private Moshi moshi;

    public MoshiConfig() {
        this.moshi = new Moshi.Builder().build();
    }

    private MoshiConfig(MoshiConfig other) {
        this.moshi = other.moshi;
    }

    /**
     * Set the {@link Moshi} to use for json conversion.
     *
     * @param moshi the mapper to use
     * @return this
     */
    public MoshiConfig setMoshi(Moshi moshi) {
        this.moshi = moshi;
        return this;
    }

    /**
     * Returns the {@link Moshi} to use for json conversion.
     *
     * @return the {@link Moshi} to use for json conversion.
     */
    public Moshi getMoshi() {
        return moshi;
    }

    @Override
    public MoshiConfig createCopy() {
        return new MoshiConfig(this);
    }
}
