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
package org.jdbi.sqlobject.config;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.jdbi.core.Handle;
import org.jdbi.core.collector.CollectorFactory;
import org.jdbi.core.junit5.H2DatabaseExtension;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class RegisterCollectorTest {

    @RegisterExtension
    H2DatabaseExtension h2 = H2DatabaseExtension.instance().withPlugin(new SqlObjectPlugin());
    Handle h;

    @BeforeEach
    void setup() {
        h = h2.getSharedHandle();
        h.execute("create table i (i int)");
        h.execute("insert into i values(1)");
        h.execute("insert into i values(2)");
    }

    @Test
    void registerCollector() {
        assertThat(h.attach(RegisterCollectorDao.class).selectWithCollector())
                .isEqualTo("1 2");
    }

    @Test
    void registerCollectorFactory() {
        assertThat(h.attach(RegisterCollectorDao.class).selectWithCollectorFactory())
            .isEqualTo("1 2");
    }

    public interface RegisterCollectorDao {
        @RegisterCollector(StringConcatCollector.class)
        @SqlQuery("select i from i order by i asc")
        String selectWithCollector();

        @RegisterCollectorFactory(StringConcatCollectorFactory.class)
        @SqlQuery("select i from i order by i asc")
        String selectWithCollectorFactory();
    }

    public static class StringConcatCollectorFactory implements CollectorFactory {

        @Override
        public boolean accepts(Type containerType) {
            return containerType == String.class;
        }

        @Override
        public Optional<Type> elementType(Type containerType) {
            return Optional.of(Integer.class);
        }

        @Override
        public Collector<Integer, List<Integer>, String> build(Type containerType) {
            return Collector.of(
                ArrayList::new,
                List::add,
                (x, y) -> {
                    x.addAll(y);
                    return x;
                },
                i -> i.stream().map(Object::toString).collect(Collectors.joining(" ")));
        }
    }

    public static class StringConcatCollector implements Collector<Integer, List<Integer>, String> {
        @Override
        public Supplier<List<Integer>> supplier() {
            return ArrayList::new;
        }

        @Override
        public BiConsumer<List<Integer>, Integer> accumulator() {
            return List::add;
        }

        @Override
        public BinaryOperator<List<Integer>> combiner() {
            return (a, b) -> {
                a.addAll(b);
                return a;
            };
        }

        @Override
        public Function<List<Integer>, String> finisher() {
            return i -> i.stream().map(Object::toString).collect(Collectors.joining(" "));
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Collections.emptySet();
        }
    }
}
