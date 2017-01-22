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
package org.jdbi.v3.sqlobject.statement;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SingleValue;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

/**
 * Helper class used by the {@link CustomizingStatementHandler}s to assemble
 * the result Collection, Iterable, etc.
 */
abstract class ResultReturner
{
    /**
     * If the return type is {@code void}, swallow results.
     * @param extensionType
     * @param method
     * @see ResultReturner#forMethod(Class, Method) if the return type is not void
     * @return
     */
    static ResultReturner forOptionalReturn(Class<?> extensionType, Method method)
    {
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
    static ResultReturner forMethod(Class<?> extensionType, Method method)
    {
        Type returnType = GenericTypes.resolveType(method.getGenericReturnType(), extensionType);
        Class<?> returnClass = getErasedType(returnType);
        if (Void.TYPE.equals(returnClass)) {
            throw new IllegalStateException(String.format(
                    "Method %s#%s is annotated as if it should return a value, but the method is void.",
                    method.getDeclaringClass().getName(),
                    method.getName()));
        }
        else if (ResultIterable.class.isAssignableFrom(returnClass)) {
            return new ResultIterableResultReturner(returnType);
        }
        else if (Stream.class.isAssignableFrom(returnClass)) {
            return new StreamReturner(returnType);
        }
        else if (Iterator.class.isAssignableFrom(returnClass)) {
            return new IteratorResultReturner(returnType);
        }
        else if (method.isAnnotationPresent(SingleValue.class)) {
            return new SingleValueResultReturner(returnType);
        }
        else {
            return new CollectedResultReturner(returnType);
        }
    }

    protected abstract Object result(ResultIterable<?> iterable, StatementContext ctx);
    protected abstract Type elementType(StatementContext ctx);

    static class VoidReturner extends ResultReturner
    {
        @Override
        protected Object result(ResultIterable<?> iterable, StatementContext ctx) {
            iterable.stream().forEach(i -> {}); // Make sure to consume the result
            return null;
        }

        @Override
        protected Type elementType(StatementContext ctx) {
            return null;
        }
    }

    static class StreamReturner extends ResultReturner
    {
        private final Type elementType;

        StreamReturner(Type returnType)
        {
            elementType = GenericTypes.findGenericParameter(returnType, Stream.class)
                    .orElseThrow(() -> new IllegalStateException(
                            "Cannot reflect Stream<T> element type T in method return type " + returnType));
        }

        @Override
        protected Stream<?> result(ResultIterable<?> iterable, StatementContext ctx) {
            return iterable.stream();
        }

        @Override
        protected Type elementType(StatementContext ctx) {
            return elementType;
        }
    }

    static class CollectedResultReturner extends ResultReturner
    {
        private final Type returnType;

        CollectedResultReturner(Type returnType)
        {
            this.returnType = returnType;
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        protected Object result(ResultIterable<?> iterable, StatementContext ctx)
        {
            Collector collector = ctx.findCollectorFor(returnType).orElse(null);
            if (collector != null) {
                return iterable.collect(collector);
            }
            return checkResult(iterable.findFirst().orElse(null), returnType);
        }

        @Override
        protected Type elementType(StatementContext ctx)
        {
            // if returnType is not supported by a collector factory, assume it to be a single-value return type.
            return ctx.findElementTypeFor(returnType).orElse(returnType);
        }
    }

    static class SingleValueResultReturner extends ResultReturner
    {
        private final Type returnType;

        SingleValueResultReturner(Type returnType)
        {
            this.returnType = returnType;
        }

        @Override
        protected Object result(ResultIterable<?> iterable, StatementContext ctx)
        {
            return checkResult(iterable.findFirst().orElse(null), returnType);
        }

        @Override
        protected Type elementType(StatementContext ctx)
        {
            return returnType;
        }
    }

    private static Object checkResult(Object result, Type type) {
        if (result == null && getErasedType(type).isPrimitive()) {
            throw new IllegalStateException("SQL method returns primitive " + type + ", but statement returned no results");
        }
        return result;
    }

    static class ResultIterableResultReturner extends ResultReturner
    {

        private final Type elementType;

        ResultIterableResultReturner(Type returnType)
        {
            // extract T from Query<T>
            elementType = GenericTypes.findGenericParameter(returnType, ResultIterable.class)
                    .orElseThrow(() -> new IllegalStateException(
                            "Cannot reflect ResultIterable<T> element type T in method return type " + returnType));
        }

        @Override
        protected Object result(ResultIterable<?> iterable, StatementContext ctx)
        {
            return iterable;
        }

        @Override
        protected Type elementType(StatementContext ctx)
        {
            return elementType;
        }
    }

    static class IteratorResultReturner extends ResultReturner
    {
        private final Type elementType;

        IteratorResultReturner(Type returnType)
        {
            this.elementType = GenericTypes.findGenericParameter(returnType, Iterator.class)
                    .orElseThrow(() -> new IllegalStateException(
                            "Cannot reflect Iterator<T> element type T in method return type " + returnType));
        }

        @Override
        protected Object result(ResultIterable<?> iterable, StatementContext ctx)
        {
            return iterable.iterator();
        }

        @Override
        protected Type elementType(StatementContext ctx)
        {
            return elementType;
        }
    }
}
