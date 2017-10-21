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
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.RowMapper;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class TestVavrTupleRowMapperFactory {

    private VavrTupleRowMapperFactory unit;

    @Before
    public void setUp() throws Exception {
        unit = new VavrTupleRowMapperFactory() {
            // mock the resolution of column mappers in jdbi
            @Override
            Optional<RowMapper<?>> getColumnMapper(Type type, int tupleIndex, ConfigRegistry config) {
                return Optional.of((rs, ctx) -> tupleIndex);
            }

            @Override
            Option<String> getConfiguredColumnName(int tupleIndex, ConfigRegistry config) {
                return Option.none();
            }

            @Override
            Array<Tuple3<Type, Integer, Option<String>>> resolveKeyValueColumns(ConfigRegistry config, Array<Tuple2<Type, Integer>> tupleTypes) {
                return tupleTypes.map(t -> Tuple.of(t._1, t._2, Option.<String>none()));
            }
        };
    }

    @Test
    public void testBuildRowMapperForUntypedTuple_shouldFail() throws SQLException {
        assertThat(unit.build(Tuple.class, null)).isEmpty();
    }

    @Test
    public void testBuildRowMapperForTuple0_shouldFail() throws SQLException {
        assertThat(unit.build(Tuple0.class, null)).isEmpty();
    }

    @Test
    public void testBuildRowMapperForTuple1_shouldSucceed() throws SQLException {
        testProjectionMapper(new GenericType<Tuple1<Integer>>() {}, Tuple.of(1));
    }

    @Test
    public void testBuildRowMapperForTuple2_shouldSucceed() throws SQLException {
        testProjectionMapper(new GenericType<Tuple2<Integer, Integer>>() {},
                Tuple.of(1, 2));
    }

    @Test
    public void testBuildRowMapperForTuple3_shouldSucceed() throws SQLException {
        testProjectionMapper(new GenericType<Tuple3<Integer, Integer, Integer>>() {},
                Tuple.of(1, 2, 3));
    }

    @Test
    public void testBuildRowMapperForTuple4_shouldSucceed() throws SQLException {
        testProjectionMapper(new GenericType<Tuple4<?, ?, ?, ?>>() {},
                Tuple.of(1, 2, 3, 4));
    }

    @Test
    public void testBuildRowMapperForTuple5_shouldSucceed() throws SQLException {
        testProjectionMapper(new GenericType<Tuple5<?, ?, ?, ?, ?>>() {},
                Tuple.of(1, 2, 3, 4, 5));
    }

    @Test
    public void testBuildRowMapperForTuple6_shouldSucceed() throws SQLException {
        testProjectionMapper(new GenericType<Tuple6<?, ?, ?, ?, ?, ?>>() {},
                Tuple.of(1, 2, 3, 4, 5, 6));
    }

    @Test
    public void testBuildRowMapperForTuple7_shouldSucceed() throws SQLException {
        testProjectionMapper(new GenericType<Tuple7<?, ?, ?, ?, ?, ?, ?>>() {},
                Tuple.of(1, 2, 3, 4, 5, 6, 7));
    }

    @Test
    public void testBuildRowMapperForTuple8_shouldSucceed() throws SQLException {
        testProjectionMapper(new GenericType<Tuple8<?, ?, ?, ?, ?, ?, ?, ?>>() {},
                Tuple.of(1, 2, 3, 4, 5, 6, 7, 8));
    }

    private void testProjectionMapper(GenericType<? extends Tuple> projection, Tuple expected) throws SQLException {
        Optional<RowMapper<?>> mapper = unit.build(projection.getType(), null);
        assertMapper(mapper, expected);
    }

    private void assertMapper(Optional<RowMapper<?>> mapper, Tuple expected) throws SQLException {
        assertThat(mapper.isPresent());
        assertThat(mapper.get()).isInstanceOf(RowMapper.class);
        assertThat(mapper.get().map(null, null)).isEqualTo(expected);
    }

}