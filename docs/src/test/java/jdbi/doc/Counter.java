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
package jdbi.doc;

import java.util.function.Function;

import org.jdbi.core.argument.Argument;
import org.jdbi.core.codec.Codec;
import org.jdbi.core.mapper.ColumnMapper;

/**
 * A simple counter class.
 */
// tag::counterCodec[]
public class Counter {

    private int count = 0;

    public Counter() {}

    public int nextValue() {
        return count++;
    }

    private Counter setValue(int value) {
        this.count = value;
        return this;
    }

    private int getValue() {
        return count;
    }

    /**
     * Codec to persist a counter to the database and restore it back.
     */
    public static class CounterCodec implements Codec<Counter> {

        @Override
        public ColumnMapper<Counter> getColumnMapper() {
            return (r, idx, ctx) -> new Counter().setValue(r.getInt(idx));
        }

        @Override
        public Function<Counter, Argument> getArgumentFunction() {
            return counter -> (idx, stmt, ctx) -> stmt.setInt(idx, counter.getValue());
        }
    }
}
// end::counterCodec[]
