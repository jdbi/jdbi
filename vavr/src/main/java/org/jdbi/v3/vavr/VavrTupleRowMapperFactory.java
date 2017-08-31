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

import io.vavr.CheckedFunction1;
import io.vavr.Tuple;
import io.vavr.Tuple0;
import io.vavr.Tuple1;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.Tuple4;
import io.vavr.Tuple5;
import io.vavr.Tuple6;
import io.vavr.Tuple7;
import io.vavr.Tuple8;
import io.vavr.collection.Array;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.mapper.SingleColumnMapper;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.Optional;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;
import static org.jdbi.v3.core.generic.GenericTypes.resolveType;

public class VavrTupleRowMapperFactory implements RowMapperFactory {

    @Override
    public Optional<RowMapper<?>> build(Type type, ConfigRegistry config) {
        Class<?> erasedType = getErasedType(type);

        boolean emptyTupleType = Tuple0.class.equals(erasedType) || Tuple.class.equals(erasedType);
        boolean mappableTupleType = type instanceof ParameterizedType && canBeMappedToTuple(erasedType);

        if (mappableTupleType && !emptyTupleType) {
            Class<? extends Tuple> tupleClass = (Class<? extends Tuple>) erasedType;
            Array<Tuple2<Type, Integer>> tupleTypes = Array.of(tupleClass.getTypeParameters())
                    .map(tp -> resolveType(tp, type))
                    .zipWithIndex();

            final Array<Optional<RowMapper<?>>> colMappers = tupleTypes
                    .map(t -> getColumnMapper(t._1, t._2 + 1, config));

            boolean mappableByColumn = colMappers.forAll(Optional::isPresent);
            if (mappableByColumn) {
                Array<? extends RowMapper<?>> cms = colMappers.map(Optional::get);
                return Optional.of((rs, ctx) ->
                        buildTuple(tupleClass, i -> cms.get(i).map(rs, ctx)));
            }

            final Array<Optional<RowMapper<?>>> rowMappers = tupleTypes
                    .map(t -> getRowMapper(t._1, t._2 + 1, config));

            boolean mappableByRowMappers = rowMappers.forAll(Optional::isPresent);
            if (mappableByRowMappers) {
                Array<? extends RowMapper<?>> rms = rowMappers.map(Optional::get);
                return Optional.of((rs, ctx) ->
                        buildTuple(tupleClass, i -> rms.get(i).map(rs, ctx)));
            }
        }

        return Optional.empty();
    }


    private boolean canBeMappedToTuple(Class<?> erasedType) {
        // tuple2 already has a mapper
        return Tuple.class.isAssignableFrom(erasedType) && !Tuple2.class.equals(erasedType);
    }

    private Tuple buildTuple(Class<? extends Tuple> tupleClass, MapperValueResolver r) throws SQLException {
        if (Tuple1.class.equals(tupleClass)) {
            return Tuple.of(r.apply(0));
        } else if (Tuple3.class.equals(tupleClass)) {
            return Tuple.of(r.apply(0), r.apply(1), r.apply(2));
        } else if (Tuple4.class.equals(tupleClass)) {
            return Tuple.of(r.apply(0), r.apply(1), r.apply(2), r.apply(3));
        } else if (Tuple5.class.equals(tupleClass)) {
            return Tuple.of(r.apply(0), r.apply(1), r.apply(2), r.apply(3), r.apply(4));
        } else if (Tuple6.class.equals(tupleClass)) {
            return Tuple.of(r.apply(0), r.apply(1), r.apply(2), r.apply(3), r.apply(4), r.apply(5));
        } else if (Tuple7.class.equals(tupleClass)) {
            return Tuple.of(r.apply(0), r.apply(1), r.apply(2), r.apply(3), r.apply(4), r.apply(5), r.apply(6));
        } else if (Tuple8.class.equals(tupleClass)) {
            return Tuple.of(r.apply(0), r.apply(1), r.apply(2), r.apply(3), r.apply(4), r.apply(5), r.apply(6), r.apply(7));
        }
        throw new IllegalArgumentException("unknown tuple type " + tupleClass);
    }

    Optional<RowMapper<?>> getColumnMapper(Type type, int tupleIndex, ConfigRegistry config) {
        int colIndex = tupleIndex;
        return config.get(ColumnMappers.class)
                .findFor(type)
                .map(cm -> new SingleColumnMapper<>(cm, colIndex));
    }

    Optional<RowMapper<?>> getRowMapper(Type type, int tupleIndex, ConfigRegistry config) {

        // try to map by configured tuple
        Optional<String> tupleColumn = Optional.ofNullable(config.get(TupleMappers.class)
                .getColumn(tupleIndex));

        if (tupleColumn.isPresent()) {
            String col = tupleColumn.get();

            return config
                    .get(ColumnMappers.class)
                    .findFor(type)
                    .map(cm -> new SingleColumnMapper<>(cm, col));

        }
        return config.get(RowMappers.class).findFor(type);
    }

    private interface MapperValueResolver extends CheckedFunction1<Integer, Object> {
        /**
         * @param tupleIndex the 1-based tuple index
         * @return the value that should be resolvable via tuple._tupleIndex
         * @throws SQLException if the underlying mapper cannot resolve the value
         */
        @Override
        Object apply(Integer tupleIndex) throws SQLException;
    }
}
