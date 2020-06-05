package org.jdbi.v3.core.result;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

final class ResultIterables {
    private ResultIterables() {}
    /**
     * Returns a ResultIterable backed by the given result set supplier, mapper, and context.
     *
     * @param supplier result set supplier
     * @param mapper   row mapper
     * @param ctx      statement context
     * @param <T>      the mapped type
     * @return the result iterable
     */
    static <T> ResultIterable<T> of(Supplier<ResultSet> supplier, RowMapper<T> mapper, StatementContext ctx) {
        return new Impl<T>(() -> {
            try {
                return new ResultSetResultIterator<>(supplier.get(), mapper, ctx);
            } catch (SQLException e) {
                try {
                    ctx.close();
                } catch (Exception e1) {
                    e.addSuppressed(e1);
                }
                throw new ResultSetException("Unable to iterator result set", e, ctx);
            }
        }, ctx);
    }

    /**
     * Returns a ResultIterable backed by the given iterator.
     * @param iterator the result iterator
     * @param <T> iterator element type
     * @return a ResultIterable
     */
    static <T> ResultIterable<T> of(Supplier<ResultIterator<T>> iterator, StatementContext ctx) {
        return new Impl<T>(iterator, ctx);
    }

    static <T> T stash(StatementContext ctx, T result) {
        if (ctx != null) { // in case someone uses the deprecated factory
            ctx.getConfig(ResultProducers.class).setResult(result);
        }
        return result;
    }

    static class Impl<T> implements ResultIterable<T> {
        private final Supplier<ResultIterator<T>> iteratorSupplier;
        private final StatementContext ctx;

        Impl(Supplier<ResultIterator<T>> iteratorSupplier, StatementContext ctx) {
            this.iteratorSupplier = iteratorSupplier;
            this.ctx = ctx;
        }

        @Override
        public ResultIterator<T> iterator() {
            return iteratorSupplier.get();
        }

        @Override
        public StatementContext statementContext() {
            return ctx;
        }
    }
}
