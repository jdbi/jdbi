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
import io.vavr.control.Option;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
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
        boolean mappableTupleType = type instanceof ParameterizedType && Tuple.class.isAssignableFrom(erasedType);

        if (mappableTupleType && !emptyTupleType) {
            Class<? extends Tuple> tupleClass = (Class<? extends Tuple>) erasedType;
            Array<Tuple2<Type, Integer>> tupleTypes = Array.of(tupleClass.getTypeParameters())
                    .map(tp -> resolveType(tp, type))
                    .zipWithIndex((t, i) -> Tuple.of(t, i + 1));

            Array<Tuple3<Type, Integer, Option<String>>> withConfiguredColumnName;
            if (Tuple2.class.equals(erasedType)) {
                withConfiguredColumnName = resolveKeyValueColumns(config, tupleTypes);
            } else {
                withConfiguredColumnName = tupleTypes
                        .map(t -> Tuple.of(t._1, t._2, getConfiguredColumnName(t._2, config)));
            }
            boolean anyColumnSet = withConfiguredColumnName.map(t -> t._3).exists(Option::isDefined);
            if (anyColumnSet) {
                Array<Optional<RowMapper<?>>> mappers = withConfiguredColumnName
                        .map(t -> t._3.isDefined() ?
                                getColumnMapperForDefinedColumn(t._1, t._3.get(), config) :
                                getRowMapper(t._1, config));

                boolean mappableWithConfigured = mappers.forAll(Optional::isPresent);
                if (mappableWithConfigured) {
                    return buildMapper(tupleClass, mappers);
                }

                Array<String> configuredColumns = withConfiguredColumnName
                        .map(t -> t._2 + ": " + t._3.getOrNull());
                throw new NoSuchMapperException(type + " cannot be mapped. " +
                        "If tuple columns are configured (TupleMappers config class), " +
                        "each tuple entry must be mappable via " +
                        "specified column name or existing RowMapper. " +
                        "Currently configured: " + configuredColumns.mkString(", "));
            } else {
                Array<Optional<RowMapper<?>>> colMappers = tupleTypes
                        .map(t -> getColumnMapper(t._1, t._2, config));

                boolean mappableByColumn = colMappers.forAll(Optional::isPresent);
                if (mappableByColumn) {
                    return buildMapper(tupleClass, colMappers);
                }

                Array<Optional<RowMapper<?>>> rowMappers = tupleTypes
                        .map(t -> getRowMapper(t._1, config));

                boolean mappableByRowMappers = rowMappers.forAll(Optional::isPresent);
                if (mappableByRowMappers) {
                    return buildMapper(tupleClass, rowMappers);
                }

                throw new NoSuchMapperException(type + " cannot be mapped. " +
                        "All tuple elements must be mappable by ColumnMapper or all by RowMapper. " +
                        "If you want to mix column- and rowmapped entries, you must configure " +
                        "columns via TupleMappers config class");
            }
        }

        return Optional.empty();
    }

    Array<Tuple3<Type, Integer, Option<String>>> resolveKeyValueColumns(ConfigRegistry config, Array<Tuple2<Type, Integer>> tupleTypes) {
        Array<Tuple3<Type, Integer, Option<String>>> withConfiguredColumnName;
        Tuple2<Type, Integer> keyType = tupleTypes.get(0);
        Tuple2<Type, Integer> valueType = tupleTypes.get(1);
        withConfiguredColumnName = Array.of(
                Tuple.of(keyType._1, keyType._2, Option.of(config.get(TupleMappers.class).getKeyColumn())),
                Tuple.of(valueType._1, valueType._2, Option.of(config.get(TupleMappers.class).getValueColumn()))
        );
        return withConfiguredColumnName;
    }

    private Optional<RowMapper<?>> buildMapper(Class<? extends Tuple> tupleClass, Array<Optional<RowMapper<?>>> colMappers) {
        Array<? extends RowMapper<?>> cms = colMappers.map(Optional::get);
        return Optional.of((rs, ctx) ->
                buildTuple(tupleClass, i -> cms.get(i).map(rs, ctx)));
    }

    private Tuple buildTuple(Class<? extends Tuple> tupleClass, MapperValueResolver r) throws SQLException {
        if (Tuple1.class.equals(tupleClass)) {
            return Tuple.of(r.apply(0));
        } else if (Tuple2.class.equals(tupleClass)) {
            return Tuple.of(r.apply(0), r.apply(1));
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

    private Optional<RowMapper<?>> getRowMapper(Type type, ConfigRegistry config) {
        return config.get(RowMappers.class).findFor(type);
    }

    private Optional<RowMapper<?>> getColumnMapperForDefinedColumn(Type type, String col, ConfigRegistry config) {
        return config
                .get(ColumnMappers.class)
                .findFor(type)
                .map(cm -> new SingleColumnMapper<>(cm, col));
    }

    Option<String> getConfiguredColumnName(int tupleIndex, ConfigRegistry config) {
        return Option.of(config.get(TupleMappers.class)
                .getColumn(tupleIndex));
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
