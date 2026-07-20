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
package org.jdbi.moshi;

import com.squareup.moshi.Moshi;
import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import org.jdbi.core.config.JdbiConfig;

/**
 * Configuration class for Moshi integration.
 */
public final class MoshiConfig implements JdbiConfig<MoshiConfig> {

    private final Moshi moshi;

    public MoshiConfig() {
        this(new Moshi.Builder().build());
    }

    private MoshiConfig(Moshi moshi) {
        this.moshi = moshi;
    }

    /**
     * Returns a copy of this configuration using the given {@link Moshi} for json conversion.
     *
     * @param moshi the mapper to use
     * @return the derived configuration
     */
    @CheckReturnValue
    public MoshiConfig moshi(Moshi moshi) {
        return new MoshiConfig(moshi);
    }

    /**
     * Returns the {@link Moshi} to use for json conversion.
     *
     * @return the {@link Moshi} to use for json conversion.
     */
    public Moshi getMoshi() {
        return moshi;
    }

}
