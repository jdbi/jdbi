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
package org.jdbi.v3.core.mapper;

import org.jdbi.v3.core.config.JdbiConfig;

/**
 * Configuration class for MapEntryMapper.
 */
public class MapEntryMappers implements JdbiConfig<MapEntryMappers>, MapEntryConfig<MapEntryMappers> {
    public MapEntryMappers() {
    }

    private MapEntryMappers(MapEntryMappers that) {
        this.keyColumn = that.keyColumn;
        this.valueColumn = that.valueColumn;
    }

    private String keyColumn;
    private String valueColumn;

    @Override
    public String getKeyColumn() {
        return keyColumn;
    }

    /**
     * Sets the column that map entry keys are loaded from. If set, keys will be loaded from the given column, using the {@link ColumnMapper} registered
     * for the key type. If unset, keys will be loaded using the {@link RowMapper} registered for the key type, from whichever columns that row mapper
     * uses.
     *
     * @param keyColumn the key column name.
     * @return this config object, for call chaining
     */
    @Override
    public MapEntryMappers setKeyColumn(String keyColumn) {
        this.keyColumn = keyColumn;
        return this;
    }

    @Override
    public String getValueColumn() {
        return valueColumn;
    }

    /**
     * Sets the column that map entry values are loaded from. If set, values will be loaded from the given column, using the {@link ColumnMapper}
     * registered for the value type. If unset, values will be loaded using the {@link RowMapper} registered for the value type, from whichever columns
     * that row mapper uses.
     *
     * @param valueColumn the value column name.
     * @return this config object, for call chaining
     */
    @Override
    public MapEntryMappers setValueColumn(String valueColumn) {
        this.valueColumn = valueColumn;
        return this;
    }

    @Override
    public MapEntryMappers createCopy() {
        return new MapEntryMappers(this);
    }
}
