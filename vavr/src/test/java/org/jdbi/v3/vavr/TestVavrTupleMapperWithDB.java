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
import io.vavr.Tuple1;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.collection.List;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.result.ResultSetException;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// TODO consider removing this since its mostly redunant with other test class
public class TestVavrTupleMapperWithDB {

    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugins();

    private List<Integer> expected = List.range(0, 9);

    @Before
    public void addData() {
        dbRule.getSharedHandle().execute("create table tuples (" +
                "t1 int, " +
                "t2 varchar(10), " +
                "t3 varchar(255), " +
                ")");
        for (Integer i : expected) {
            dbRule.getSharedHandle().execute("insert into tuples values (?, ?, ?)",
                    i, "t2" + i, "t3" + (i + 1));
        }
    }

    @Test
    public void testMapToTuple1_shouldSucceed() {
        Tuple1<String> tupleProjection = dbRule.getSharedHandle()
                .createQuery("select t2 from tuples order by t1 asc")
                .mapTo(new GenericType<Tuple1<String>>() {})
                .findFirst().get();

        assertThat(tupleProjection).isEqualTo(Tuple.of("t20"));
    }

    @Test
    public void testTuple1CollectorWithSingleSelect_shouldSucceed() {
        List<Tuple1<String>> expectedTuples = expected.map(i -> new Tuple1<>("t2" + i));
        List<Tuple1<String>> tupleProjection = dbRule.getSharedHandle()
                .createQuery("select t2 from tuples")
                .collectInto(new GenericType<List<Tuple1<String>>>() {});

        assertThat(tupleProjection).containsOnlyElementsOf(expectedTuples);
    }

    @Test
    public void testTuple1CollectorWithMultiSelect_shouldSucceed() {
        List<Tuple1<Integer>> firstColumnTuples = expected.map(Tuple1::new);
        List<Tuple1<Integer>> tupleProjection = dbRule.getSharedHandle()
                .createQuery("select * from tuples")
                .collectInto(new GenericType<List<Tuple1<Integer>>>() {});

        assertThat(tupleProjection).containsOnlyElementsOf(firstColumnTuples);
    }

    @Test
    public void testTuple1CollectorWithMultiSelect_shouldFail() {
        // first selection is not projectable to tuple param
        assertThatThrownBy(() -> dbRule.getSharedHandle()
            .createQuery("select t2, t3 from tuples")
            .collectInto(new GenericType<List<Tuple1<Integer>>>() {})).isInstanceOf(ResultSetException.class);
    }

    @Test
    public void testMapToTuple2List_shouldSucceed() {
        List<Tuple2<Integer, String>> expectedTuples = expected.map(i -> new Tuple2<>(i, "t2" + i));
        java.util.List<Tuple2<Integer, String>> tupleProjection = dbRule.getSharedHandle()
                .createQuery("select t1, t2 from tuples")
                .mapTo(new GenericType<Tuple2<Integer, String>>() {}).list();

        assertThat(tupleProjection).containsOnlyElementsOf(expectedTuples);
    }

    @Test
    public void testTuple3CollectorWithSelectedKeyValue_shouldSucceed() {
        List<Tuple3<Integer, String, String>> expectedTuples = expected.map(i -> new Tuple3<>(i, "t2" + i, "t3" + (i + 1)));
        List<Tuple3<Integer, String, String>> tupleProjection = dbRule.getSharedHandle()
                .createQuery("select t1, t2, t3 from tuples")
                .collectInto(new GenericType<List<Tuple3<Integer, String, String>>>() {});

        assertThat(tupleProjection).containsOnlyElementsOf(expectedTuples);
    }

}
