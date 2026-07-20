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

import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import org.jdbi.core.config.JdbiConfig;

/**
 * Configuration class for MapEntryMapper.
 * <p>
 * This configuration is immutable: {@link #keyColumn} and {@link #valueColumn} return a new instance, leaving
 * the receiver unchanged.
 */
public final class MapEntryMappers implements JdbiConfig<MapEntryMappers>, MapEntryConfig<MapEntryMappers> {

    private final String keyColumn;
    private final String valueColumn;

    public MapEntryMappers() {
        this(null, null);
    }

    private MapEntryMappers(String keyColumn, String valueColumn) {
        this.keyColumn = keyColumn;
        this.valueColumn = valueColumn;
    }

    @Override
    public String getKeyColumn() {
        return keyColumn;
    }

    @CheckReturnValue
    @Override
    public MapEntryMappers keyColumn(String keyColumn) {
        return new MapEntryMappers(keyColumn, valueColumn);
    }

    @Override
    public String getValueColumn() {
        return valueColumn;
    }

    @CheckReturnValue
    @Override
    public MapEntryMappers valueColumn(String valueColumn) {
        return new MapEntryMappers(keyColumn, valueColumn);
    }

}
