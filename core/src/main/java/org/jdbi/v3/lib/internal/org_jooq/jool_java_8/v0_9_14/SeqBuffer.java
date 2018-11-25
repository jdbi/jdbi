package org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Lazily consumes given <code>Spliterator</code> through <code>Seq</code>s provided by method <code>seq()</code>.
 * This method may be called multiple times, and the returned <code>Seq</code>s may be consumed interchangeably.
 *
 * Instances of this class ARE thread-safe.
 *
 * @author Tomasz Linkowski
 */
final class SeqBuffer<T> {

    @SuppressWarnings("unchecked")
    static <T> SeqBuffer<T> of(Stream<? extends T> stream) {
        return of((Spliterator<T>) stream.spliterator());
    }

    static <T> SeqBuffer<T> of(Spliterator<T> spliterator) {
        if (spliterator instanceof SeqBuffer.BufferSpliterator) {
            return ((SeqBuffer<T>.BufferSpliterator) spliterator).parentSeqBuffer(); // reuse existing SeqBuffer
        }
        return new SeqBuffer<>(spliterator);
    }

    private final Spliterator<T> source;
    private final List<T> buffer = new ArrayList<>();

    /**
     * <code>True</code> while <code>source</code> hasn't reported that it's exhausted.
     *
     * This volatile field acts as a memory barrier for the contents of <code>source</code> and <code>buffer</code>:
     *
     * @link http://www.cs.umd.edu/~pugh/java/memoryModel/jsr-133-faq.html#volatile
     */
    private volatile boolean buffering = true;

    private SeqBuffer(Spliterator<T> source) {
        this.source = Objects.requireNonNull(source);
    }

    /**
     * Returns a <code>Seq</code> over given <code>source</code>.
     *
     * Although such <code>Seq</code> is not thread-safe by itself, it interacts in a thread-safe way with its
     * parent <code>SeqBuffer</code>. As a result, each <code>Seq</code> returned by this method can be safely
     * used on a different thread.
     */
    Seq<T> seq() {
        return Seq.seq(new BufferSpliterator());
    }

    /**
     * Special <code>Spliterator</code> whose <code>tryAdvance</code> method can buffer
     * (i.e. can advance the <code>source</code> spliterator).
     *
     * Instances of this class are NOT thread-safe but they interact in a thread-safe way with <code>SeqBuffer</code>.
     */
    private class BufferSpliterator implements Spliterator<T> {

        /**
         * Index of the element that will be returned upon next call to <code>tryAdvance</code> if such element exists.
         */
        private int nextIndex = 0;

        //
        // TRY ADVANCE
        //
        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            return buffering // volatile-read (ensures buffer is up-to-date)
                  ? tryAdvanceThisWithBuffering(action) // slow (synchronized)
                  : tryAdvanceThisAtOnce(action); // fast (not synchronized)
        }

        /**
         * Tries to advance this <code>Spliterator</code> to element at <code>nextIndex</code>,
         * buffering <code>source</code> elements into <code>buffer</code> if necessary.
         *
         * Synchronized on <code>buffer</code> in order to:
         * - obtain accurate <code>buffer.size()</code>
         * - safely copy from <code>source</code> to <code>buffer</code> (if needed)
         * - safely call <code>buffer.get()</code>
         */
        private boolean tryAdvanceThisWithBuffering(Consumer<? super T> action) {
            final T next;
            synchronized (buffer) {
                if (!canAdvanceThisWithBuffering())
                    return false;

                next = advanceThis();
            }

            action.accept(next); // call "action" outside of synchronized block
            return true;
        }

        private boolean canAdvanceThisWithBuffering() {
            return canAdvanceThisAtOnce() || tryAdvanceSource() && canAdvanceThisAtOnce();
        }

        private boolean canAdvanceThisAtOnce() {
            return nextIndex < buffer.size();
        }

        /**
         * Buffers (i.e. advances the <code>source</code>) until an item at <code>nextIndex</code>
         * is added to the <code>buffer</code>, or until the <code>source</code> is exhausted.
         *
         * Guarded by: <code>buffer</code>
         */
        private boolean tryAdvanceSource() {
            boolean canAdvanceSource = buffering; // volatile-read (stored in local variable to limit R/W operations)
            if (!canAdvanceSource) // check again after having synchronized
                return false;

            do {
                canAdvanceSource = source.tryAdvance(buffer::add);
            } while (canAdvanceSource && !canAdvanceThisAtOnce());

            // volatile-write (causes grown buffer and shrunk source to be visible to all threads upon next volatile-read)
            buffering = canAdvanceSource;
            return true;
        }

        private T advanceThis() {
            return buffer.get(nextIndex++);
        }

        /**
         * Called only when buffering has been completed.
         */
        private boolean tryAdvanceThisAtOnce(Consumer<? super T> action) {
            if (!canAdvanceThisAtOnce())
                return false;

            action.accept(advanceThis());
            return true;
        }

        //
        // ESTIMATE SIZE
        //
        @Override
        public long estimateSize() {
            return buffering // volatile-read (ensures buffer is up-to-date)
                  ? estimateSizeDuringBuffering() // slow (synchronized)
                  : numberOfElementsLeftInBuffer(); // fast (not synchronized)
        }

        /**
         * Returns the estimate size of this Spliterator.
         *
         * Synchronized to get an accurate sum of <code>buffer.size()</code> and <code>source.estimateSize()</code>.
         */
        private long estimateSizeDuringBuffering() {
            synchronized (buffer) {
                int leftInBuffer = numberOfElementsLeftInBuffer();
                if (!buffering) // check again after having synchronized
                    return leftInBuffer;

                long estimateSize = leftInBuffer + source.estimateSize();
                // will overflow to negative number if source.estimateSize() reports Long.MAX_VALUE
                return estimateSize >= 0 ? estimateSize : Long.MAX_VALUE;
            }
        }

        private int numberOfElementsLeftInBuffer() {
            return buffer.size() - nextIndex;
        }

        //
        // REMAINING
        //
        @Override
        public Spliterator<T> trySplit() {
            return null;
        }

        @Override
        public int characteristics() {
            // no synchronization here because source.characteristics() is assumed to be thread-safe
            return (source.characteristics() & ~Spliterator.CONCURRENT) | Spliterator.ORDERED
                  | (buffering ? 0 : Spliterator.SIZED);
        }

        @Override
        public Comparator<? super T> getComparator() {
            return source.getComparator();
        }

        SeqBuffer<T> parentSeqBuffer() {
            return SeqBuffer.this;
        }
    }
}
