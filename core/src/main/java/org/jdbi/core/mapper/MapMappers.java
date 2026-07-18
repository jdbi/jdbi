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
package org.jdbi.core.mapper;

import java.util.function.UnaryOperator;

import org.jdbi.core.config.JdbiConfig;

public final class MapMappers implements JdbiConfig<MapMappers> {

    private final UnaryOperator<String> caseChange;

    public MapMappers() {
        this(CaseStrategy.LOCALE_LOWER);
    }

    private MapMappers(UnaryOperator<String> caseChange) {
        this.caseChange = caseChange;
    }

    /**
     * Case change strategy for the database column names. By default, the row names are lowercased using the system locale.
     *
     * @return The current case change strategy.
     * @see CaseStrategy
     */
    public UnaryOperator<String> getCaseChange() {
        return caseChange;
    }

    /**
     * Returns a copy of this configuration with the given case change strategy for the database column names.
     * By default, the row names are lowercased using the system locale.
     *
     * @param caseChange The strategy to use. Must not be null.
     * @return the derived configuration
     * @see CaseStrategy
     */
    public MapMappers caseChange(UnaryOperator<String> caseChange) {
        return new MapMappers(caseChange);
    }

    @Override
    public MapMappers createCopy() {
        // Immutable: safe to share across registries.
        return this;
    }
}
