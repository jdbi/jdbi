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
package org.jdbi.v3.sqlobject.statement.internal;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.result.ResultIterator;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SingleValue;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

/**
 * Helper class used by the {@link CustomizingStatementHandler}s to assemble
 * the result Collection, Iterable, etc.
 */
abstract class ResultReturner {
    /**
     * If the return type is {@code void}, swallow results.
     * @param extensionType
     * @param method
     * @see ResultReturner#forMethod(Class, Method) if the return type is not void
     * @return
     */
    static ResultReturner forOptionalReturn(Class<?> extensionType, Method method) {
        if (method.getReturnType() == void.class) {
            return new VoidReturner();
        }
        return forMethod(extensionType, method);
    }

    /**
     * Inspect a Method for its return type, and choose a ResultReturner subclass
     * that handles any container that might wrap the results.
     * @param extensionType the type that owns the Method
     * @param method the method whose return type chooses the ResultReturner
     * @return an instance that takes a ResultIterable and constructs the return value
     */
    static ResultReturner forMethod(Class<?> extensionType, Method method) {
        Type returnType = GenericTypes.resolveType(method.getGenericReturnType(), extensionType);
        QualifiedType<?> qualifiedReturnType = QualifiedType.of(returnType).withAnnotations(new Qualifiers().findFor(method));
        Class<?> returnClass = getErasedType(returnType);
        if (Void.TYPE.equals(returnClass)) {
            return findConsumer(method)
                .orElseThrow(() -> new IllegalStateException(String.format(
                    "Method %s#%s is annotated as if it should return a value, but the method is void.",
                    method.getDeclaringClass().getName(),
                    method.getName())));
        } else if (ResultIterable.class.equals(returnClass)) {
            return new ResultIterableReturner(qualifiedReturnType);
        } else if (Stream.class.equals(returnClass)) {
            return new StreamReturner(qualifiedReturnType);
        } else if (ResultIterator.class.equals(returnClass)) {
            return new ResultIteratorReturner(qualifiedReturnType);
        } else if (Iterator.class.equals(returnClass)) {
            return new IteratorReturner(qualifiedReturnType);
        } else if (method.isAnnotationPresent(SingleValue.class)) {
            return new SingleValueReturner(qualifiedReturnType);
        } else {
            return new CollectedResultReturner(qualifiedReturnType);
        }
    }

    /**
     * Inspect a Method for a {@link Consumer} to execute for each produced row.
     * @param method the method called
     * @return a ResultReturner that invokes the consumer and does not return a value
     */
    static Optional<ResultReturner> findConsumer(Method method) {
        final Class<?>[] paramTypes = method.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (paramTypes[i] == Consumer.class) {
                return Optional.of(new ConsumerResultReturner(method, i));
            }
        }
        return Optional.empty();
    }

    protected abstract Object mappedResult(ResultIterable<?> iterable, StatementContext ctx);
    protected abstract Object reducedResult(Stream<?> stream, StatementContext ctx);

    protected abstract QualifiedType<?> elementType(StatementContext ctx);

    private static Object checkResult(Object result, QualifiedType<?> type) {
        if (result == null && getErasedType(type.getType()).isPrimitive()) {
            throw new IllegalStateException("SQL method returns primitive " + type + ", but statement returned no results");
        }
        return result;
    }

    static class VoidReturner extends ResultReturner {
        @Override
        protected Void mappedResult(ResultIterable<?> iterable, StatementContext ctx) {
            iterable.stream().forEach(i -> {}); // Make sure to consume the result
            return null;
        }

        @Override
        protected Void reducedResult(Stream<?> stream, StatementContext ctx) {
            throw new UnsupportedOperationException("Cannot return void from a @UseRowReducer method");
        }

        @Override
        protected QualifiedType<?> elementType(StatementContext ctx) {
            return null;
        }
    }

    static class ResultIterableReturner extends ResultReturner {

        private final QualifiedType<?> elementType;

        ResultIterableReturner(QualifiedType<?> returnType) {
            // extract T from Query<T>
            elementType = returnType.flatMapType(type -> GenericTypes.findGenericParameter(type, ResultIterable.class))
                .orElseThrow(() -> new IllegalStateException(
                    "Cannot reflect ResultIterable<T> element type T in method return type " + returnType));
        }

        @Override
        protected ResultIterable<?> mappedResult(ResultIterable<?> iterable, StatementContext ctx) {
            return iterable;
        }

        @Override
        protected ResultIterator<?> reducedResult(Stream<?> stream, StatementContext ctx) {
            throw new UnsupportedOperationException("Cannot return ResultIterable from a @UseRowReducer method");
        }

        @Override
        protected QualifiedType<?> elementType(StatementContext ctx) {
            return elementType;
        }
    }

    static class StreamReturner extends ResultReturner {
        private final QualifiedType<?> elementType;

        StreamReturner(QualifiedType<?> returnType) {
            elementType = returnType.flatMapType(type -> GenericTypes.findGenericParameter(type, Stream.class))
                .orElseThrow(() -> new IllegalStateException(
                    "Cannot reflect Stream<T> element type T in method return type " + returnType));
        }

        @Override
        protected Stream<?> mappedResult(ResultIterable<?> iterable, StatementContext ctx) {
            return iterable.stream();
        }

        @Override
        protected Stream<?> reducedResult(Stream<?> stream, StatementContext ctx) {
            return stream;
        }

        @Override
        protected QualifiedType<?> elementType(StatementContext ctx) {
            return elementType;
        }
    }

    static class ResultIteratorReturner extends ResultReturner {
        private final QualifiedType<?> elementType;

        ResultIteratorReturner(QualifiedType<?> returnType) {
            this.elementType = returnType.flatMapType(type -> GenericTypes.findGenericParameter(type, Iterator.class))
                .orElseThrow(() -> new IllegalStateException(
                    "Cannot reflect ResultIterator<T> element type T in method return type " + returnType));
        }

        @Override
        protected ResultIterator<?> mappedResult(ResultIterable<?> iterable, StatementContext ctx) {
            return iterable.iterator();
        }

        @Override
        protected ResultIterator<?> reducedResult(Stream<?> stream, StatementContext ctx) {
            throw new UnsupportedOperationException("Cannot return ResultIterator from a @UseRowReducer method");
        }

        @Override
        protected QualifiedType<?> elementType(StatementContext ctx) {
            return elementType;
        }
    }

    static class IteratorReturner extends ResultReturner {
        private final QualifiedType<?> elementType;

        IteratorReturner(QualifiedType<?> returnType) {
            this.elementType = returnType.flatMapType(type -> GenericTypes.findGenericParameter(type, Iterator.class))
                .orElseThrow(() -> new IllegalStateException(
                    "Cannot reflect Iterator<T> element type T in method return type " + returnType));
        }

        @Override
        protected Iterator<?> mappedResult(ResultIterable<?> iterable, StatementContext ctx) {
            return iterable.iterator();
        }

        @Override
        protected Iterator<?> reducedResult(Stream<?> stream, StatementContext ctx) {
            return stream.iterator();
        }

        @Override
        protected QualifiedType<?> elementType(StatementContext ctx) {
            return elementType;
        }
    }

    static class SingleValueReturner<T> extends ResultReturner {
        private final QualifiedType<T> returnType;

        SingleValueReturner(QualifiedType<T> returnType) {
            this.returnType = returnType;
        }

        @Override
        protected Object mappedResult(ResultIterable<?> iterable, StatementContext ctx) {
            return checkResult(iterable.findFirst().orElse(null), returnType);
        }

        @Override
        protected Object reducedResult(Stream<?> stream, StatementContext ctx) {
            return checkResult(stream.findFirst().orElse(null), returnType);
        }

        @Override
        protected QualifiedType<T> elementType(StatementContext ctx) {
            return returnType;
        }
    }

    static class CollectedResultReturner<T> extends ResultReturner {
        private final QualifiedType<T> returnType;

        CollectedResultReturner(QualifiedType<T> returnType) {
            this.returnType = returnType;
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        protected Object mappedResult(ResultIterable<?> iterable, StatementContext ctx) {
            Collector collector = ctx.findCollectorFor(returnType.getType()).orElse(null);
            if (collector != null) {
                return iterable.collect(collector);
            }
            return checkResult(iterable.findFirst().orElse(null), returnType);
        }

        @Override
        protected Object reducedResult(Stream<?> stream, StatementContext ctx) {
            Collector collector = ctx.findCollectorFor(returnType.getType()).orElse(null);
            if (collector != null) {
                return stream.collect(collector);
            }
            return checkResult(stream.findFirst().orElse(null), returnType);
        }

        @Override
        protected QualifiedType<?> elementType(StatementContext ctx) {
            // if returnType is not supported by a collector factory, assume it to be a single-value return type.
            return returnType.flatMapType(type -> ctx.findElementTypeFor(type))
                .orElse(returnType);
        }
    }

    static class ConsumerResultReturner extends ResultReturner {
        private final int consumerIndex;
        private final QualifiedType<?> elementType;

        ConsumerResultReturner(Method method, int consumerIndex) {
            this.consumerIndex = consumerIndex;
            Type parameterType = method.getGenericParameterTypes()[consumerIndex];
            this.elementType = QualifiedType.of(
                GenericTypes.findGenericParameter(parameterType, Consumer.class)
                    .orElseThrow(() -> new IllegalStateException(
                        "Cannot reflect Consumer<T> element type T in method consumer parameter "
                            + parameterType)))
                .withAnnotations(new Qualifiers().findFor(method.getParameters()[consumerIndex]));
        }

        @Override
        protected Void mappedResult(ResultIterable<?> iterable, StatementContext ctx) {
            @SuppressWarnings("unchecked")
            Consumer<Object> consumer = (Consumer<Object>)
                ctx.getConfig(SqlObjectStatementConfiguration.class).getArgs()[consumerIndex];
            iterable.forEach(consumer);
            return null;
        }

        @Override
        protected Void reducedResult(Stream<?> stream, StatementContext ctx) {
            @SuppressWarnings("unchecked")
            Consumer<Object> consumer = (Consumer<Object>)
                ctx.getConfig(SqlObjectStatementConfiguration.class).getArgs()[consumerIndex];
            stream.forEach(consumer);
            return null;
        }

        @Override
        protected QualifiedType<?> elementType(StatementContext ctx) {
            return elementType;
        }
    }
}
