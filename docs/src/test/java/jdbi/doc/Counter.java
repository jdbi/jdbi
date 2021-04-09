package jdbi.doc;

import java.util.function.Function;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.codec.Codec;
import org.jdbi.v3.core.mapper.ColumnMapper;

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
