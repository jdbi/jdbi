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
package org.jdbi.v3.vavr;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.mapper.MapEntryConfig;
import org.jdbi.v3.core.mapper.MapEntryMappers;

/**
 * similar to {@link org.jdbi.v3.core.mapper.MapEntryMappers} but map entries in vavr are in fact
 * of type Tuple2
 */
public class TupleMappers implements JdbiConfig<TupleMappers>, MapEntryConfig<TupleMappers> {

    private static final int KEY_COLUMN_TUPLE_INDEX = 1;
    private static final int VALUE_COLUMN_TUPLE_INDEX = 2;

    private ConfigRegistry registry;

    private String[] columns = new String[8];

    public TupleMappers() {
    }

    private TupleMappers(TupleMappers that) {
        this.columns = that.columns;
    }

    @Override
    public String getKeyColumn() {
        String column = getColumn(KEY_COLUMN_TUPLE_INDEX);
        if (column == null) {
            // fallback to global map key config
            return this.registry.get(MapEntryMappers.class).getKeyColumn();
        }
        return column;
    }

    @Override
    public TupleMappers setKeyColumn(String keyColumn) {
        return setColumn(KEY_COLUMN_TUPLE_INDEX, keyColumn);
    }

    @Override
    public String getValueColumn() {
        String column = getColumn(VALUE_COLUMN_TUPLE_INDEX);
        if (column == null) {
            // fallback to global map value config
            return this.registry.get(MapEntryMappers.class).getValueColumn();
        }
        return column;
    }

    @Override
    public TupleMappers setValueColumn(String valueColumn) {
        return setColumn(VALUE_COLUMN_TUPLE_INDEX, valueColumn);
    }

    /**
     * @param tupleIndex the 1 based index of the TupleX. as in _1, _2 etc.
     * @param name       the column name to be mapped explicitly
     * @return Config object for chaining
     */
    public TupleMappers setColumn(int tupleIndex, String name) {
        columns[tupleIndex - 1] = name;
        return this;
    }

    /**
     * @param tupleIndex the 1 based index of the TupleX. as in _1, _2 etc.
     * @return the column name to be mapped explicitly
     */
    public String getColumn(int tupleIndex) {
        return columns[tupleIndex - 1];
    }

    @Override
    public TupleMappers createCopy() {
        return new TupleMappers(this);
    }

    @Override
    public void setRegistry(ConfigRegistry registry) {
        this.registry = registry;
    }

}
