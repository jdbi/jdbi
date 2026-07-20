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
package org.jdbi.core.enums;

import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import org.jdbi.core.config.JdbiConfig;

/**
 * Configuration for behavior related to {@link Enum}s.
 */
public final class Enums implements JdbiConfig<Enums> {
    private final EnumStrategy strategy;

    public Enums() {
        this(EnumStrategy.BY_NAME);
    }

    private Enums(EnumStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Returns the default strategy to use for mapping and binding enums, in the absence of a
     * {@link EnumByName} or {@link EnumByOrdinal} qualifying annotation. The default default
     * is {@link EnumStrategy#BY_NAME}.
     */
    public EnumStrategy getDefaultStrategy() {
        return strategy;
    }

    /**
     * Returns a copy of this configuration with the given default strategy for mapping and binding enums.
     * @param enumStrategy the new strategy
     * @return the derived configuration
     */
    @CheckReturnValue
    public Enums defaultStrategy(EnumStrategy enumStrategy) {
        return new Enums(enumStrategy);
    }

}
