package org.jdbi.v3.core.mapper;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import org.jdbi.v3.core.collector.CollectorFactory;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.JoinMapperTest.Author;
import org.jdbi.v3.core.mapper.JoinMapperTest.Book;
import org.jdbi.v3.core.result.RowView;
import org.jdbi.v3.core.statement.StatementContext;

public class JoinCollector<T, A, R> implements CollectorFactory {

    private final Type resultType;

    JoinCollector(Supplier<A> accumulatorType, Type resultType) {
        this.resultType = resultType;
    }

    @Override
    public boolean accepts(Type containerType) {
        return resultType.equals(containerType);
    }

    @Override
    public Optional<Type> elementType(Type containerType) {
        return Optional.of(RowView.class);
    }

    @Override
    public Collector<?, ?, ?> build(Type containerType) {
        return Collector.of(supplier, accumulator, combiner, finisher);
    }

    public static JoinCollector.Builder natural(Class<?> collectedInto) {
        return natural((Type) collectedInto);
    }

    public static JoinCollector.Builder natural(GenericType<?> collectedInto) {
        return natural((Type) collectedInto);
    }

    public static JoinCollector.Builder natural(Type collectedInto) {
        return new Builder(collectedInto);
    }

    public static class Builder {
        private final Type leftType;

        Builder(Type leftType) {
            this.leftType = leftType;
        }

//        public <T, R> JoinCollector<T, R, R> into(Type intoType) {
//            return into(ArrayList::new, Function.identity(), intoType);
//        }

        public <T, A, R> JoinCollector<T, A, R> into(
                Supplier<A> accumulator,
                Function<A, R> finisher,
                Type intoType) {
            return new JoinCollector<>(accumulator, intoType);
        }
    }
}
