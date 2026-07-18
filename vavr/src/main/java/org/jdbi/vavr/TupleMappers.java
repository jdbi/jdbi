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
package org.jdbi.vavr;

import io.vavr.Tuple;
import org.jdbi.core.config.JdbiConfig;
import org.jdbi.core.mapper.MapEntryConfig;

/**
 * Mappers similar to {@link org.jdbi.core.mapper.MapEntryMappers} but map entries in vavr are in fact
 * of type {@link io.vavr.Tuple2}.
 * <p>
 * This configuration is immutable: {@link #keyColumn}, {@link #valueColumn} and {@link #column} return a new
 * instance, leaving the receiver unchanged. Only tuple-specific columns are stored here; a caller that wants
 * to honor the global {@link org.jdbi.core.mapper.MapEntryMappers} column names falls back to them itself
 * (see {@link VavrTupleRowMapperFactory}).
 */
public final class TupleMappers implements JdbiConfig<TupleMappers>, MapEntryConfig<TupleMappers> {

    private static final int KEY_COLUMN_TUPLE_INDEX = 1;
    private static final int VALUE_COLUMN_TUPLE_INDEX = 2;

    private final String[] columns;

    public TupleMappers() {
        this(new String[Tuple.MAX_ARITY]);
    }

    private TupleMappers(String[] columns) {
        this.columns = columns;
    }

    @Override
    public String getKeyColumn() {
        return getColumn(KEY_COLUMN_TUPLE_INDEX);
    }

    @Override
    public TupleMappers keyColumn(String keyColumn) {
        return column(KEY_COLUMN_TUPLE_INDEX, keyColumn);
    }

    @Override
    public String getValueColumn() {
        return getColumn(VALUE_COLUMN_TUPLE_INDEX);
    }

    @Override
    public TupleMappers valueColumn(String valueColumn) {
        return column(VALUE_COLUMN_TUPLE_INDEX, valueColumn);
    }

    /**
     * Returns a copy of this configuration with the given column name for a specific tuple position.
     *
     * @param tupleIndex the 1 based index of the TupleX. as in _1, _2 etc.
     * @param name       the column name to be mapped explicitly
     * @return the derived configuration
     */
    public TupleMappers column(int tupleIndex, String name) {
        final String[] newColumns = columns.clone();
        newColumns[tupleIndex - 1] = name;
        return new TupleMappers(newColumns);
    }

    /**
     * Returns the name for a column.
     *
     * @param tupleIndex the 1 based index of the TupleX. as in _1, _2 etc.
     * @return the column name to be mapped explicitly
     */
    public String getColumn(int tupleIndex) {
        return columns[tupleIndex - 1];
    }

    @Override
    public TupleMappers createCopy() {
        // Immutable: safe to share across registries.
        return this;
    }
}
