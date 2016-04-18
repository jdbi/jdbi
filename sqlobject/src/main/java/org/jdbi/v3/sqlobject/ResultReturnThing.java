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
package org.jdbi.v3.sqlobject;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.jdbi.v3.Handle;
import org.jdbi.v3.Query;
import org.jdbi.v3.ResultBearing;
import org.jdbi.v3.ResultIterator;
import org.jdbi.v3.Types;
import org.jdbi.v3.exceptions.UnableToCreateStatementException;
import org.jdbi.v3.sqlobject.customizers.SingleValueResult;
import org.jdbi.v3.sqlobject.customizers.UseRowMapper;
import org.jdbi.v3.tweak.RowMapper;

abstract class ResultReturnThing
{
    public Object map(Method method, Query<?> q, Supplier<Handle> handle)
    {
        if (method.isAnnotationPresent(UseRowMapper.class)) {
            final RowMapper<?> mapper;
            try {
                mapper = method.getAnnotation(UseRowMapper.class).value().newInstance();
            }
            catch (Exception e) {
                throw new UnableToCreateStatementException("unable to access mapper", e, null);
            }
            return result(q.map(mapper), handle);
        }
        else {
            return result(q.mapTo(elementType()), handle);
        }
    }

    static ResultReturnThing forMethod(Class<?> extensionType, Method method)
    {
        Type returnType = Types.resolveType(method.getGenericReturnType(), extensionType);
        Class returnClass = Types.getErasedType(returnType);
        if (Void.TYPE.equals(returnClass)) {
            throw new IllegalStateException(String.format(
                    "Method %s#%s is annotated as if it should return a value, but the method is void.",
                    method.getDeclaringClass().getName(),
                    method.getName()));
        }
        else if (ResultBearing.class.isAssignableFrom(returnClass)) {
            return new ResultBearingResultReturnThing(returnType);
        }
        else if (Stream.class.isAssignableFrom(returnClass)) {
            return new StreamReturnThing(returnType);
        }
        else if (Iterable.class.isAssignableFrom(returnClass)) {
            return new IterableReturningThing(returnType);
        }
        else if (Iterator.class.isAssignableFrom(returnClass)) {
            return new IteratorResultReturnThing(returnType);
        }
        else {
            return new SingleValueResultReturnThing(method, returnType);
        }
    }

    protected abstract Object result(ResultBearing<?> q, Supplier<Handle> handle);

    protected abstract Type elementType();

    static class StreamReturnThing extends ResultReturnThing
    {
        private final Type elementType;

        StreamReturnThing(Type returnType)
        {
            elementType = Types.findGenericParameter(returnType, Stream.class)
                    .orElseThrow(() -> new IllegalStateException(
                            "Cannot reflect Stream<T> element type T in method return type " + returnType));
        }

        @Override
        protected Object result(ResultBearing<?> q, Supplier<Handle> handle) {
            return q.stream();
        }

        @Override
        protected Type elementType() {
            return elementType;
        }
    }

    static class SingleValueResultReturnThing extends ResultReturnThing
    {
        private final Type elementType;
        private final Type containerType;

        SingleValueResultReturnThing(Method method, Type returnType)
        {
            if (method.isAnnotationPresent(SingleValueResult.class)) {
                SingleValueResult svr = method.getAnnotation(SingleValueResult.class);
                // try to guess generic type
                if(SingleValueResult.Default.class == svr.value()){
                    Class<?> erasedReturnType = Types.getErasedType(returnType);
                    elementType = Types.findGenericParameter(returnType, erasedReturnType)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Ambiguous generic information. SingleValueResult type could not be fetched."));

                }else{
                    elementType = svr.value();
                }
                this.containerType = returnType;
            }
            else {
                this.elementType = returnType;
                this.containerType = null;
            }

        }

        @Override
        @SuppressWarnings("unchecked")
        protected Object result(ResultBearing<?> q, Supplier<Handle> handle)
        {
            if (containerType != null) {
                Collector collector = ((Query)q).getContext().findCollectorFor(containerType)
                        .orElseThrow(() -> new IllegalStateException("No collector available for " + containerType));
                return q.collect(collector);
            }
            return q.findFirst().orElse(null);
        }

        @Override
        protected Type elementType()
        {
            return elementType;
        }
    }

    static class ResultBearingResultReturnThing extends ResultReturnThing
    {

        private final Type elementType;

        ResultBearingResultReturnThing(Type returnType)
        {
            // extract T from Query<T>
            elementType = Types.findGenericParameter(returnType, Query.class)
                    .orElseThrow(() -> new IllegalStateException(
                            "Cannot reflect Query<T> element type T in method return type " + returnType));
        }

        @Override
        protected Object result(ResultBearing<?> q, Supplier<Handle> handle)
        {
            return q;
        }

        @Override
        protected Type elementType()
        {
            return elementType;
        }
    }

    static class IteratorResultReturnThing extends ResultReturnThing
    {
        private final Type elementType;

        IteratorResultReturnThing(Type returnType)
        {
            this.elementType = Types.findGenericParameter(returnType, Iterator.class)
                    .orElseThrow(() -> new IllegalStateException(
                            "Cannot reflect Iterator<T> element type T in method return type " + returnType));
        }

        @Override
        protected Object result(ResultBearing<?> q, final Supplier<Handle> handle)
        {
            final ResultIterator<?> itty = q.iterator();

            final boolean isEmpty = !itty.hasNext();
            if (isEmpty) {
                itty.close();
            }

            return new ResultIterator<Object>()
            {
                private boolean closed = isEmpty;
                private boolean hasNext = !isEmpty;

                @Override
                public void close()
                {
                    if (!closed) {
                        closed = true;
                        itty.close();
                    }
                }

                @Override
                public boolean hasNext()
                {
                    return hasNext;
                }

                @Override
                public Object next()
                {
                    Object rs;
                    try {
                        rs = itty.next();
                        hasNext = itty.hasNext();
                    }
                    catch (RuntimeException e) {
                        closeIgnoreException();
                        throw e;
                    }
                    if (!hasNext) {
                        close();
                    }
                    return rs;
                }

                @SuppressWarnings("PMD.EmptyCatchBlock")
                public void closeIgnoreException() {
                    try {
                        close();
                    } catch (RuntimeException ex) {}
                }

                @Override
                public void remove()
                {
                    itty.remove();
                }
            };
        }

        @Override
        protected Type elementType()
        {
            return elementType;
        }
    }

    static class IterableReturningThing extends ResultReturnThing
    {
        private final Type iterableType;
        private final Type elementType;

        IterableReturningThing(Type returnType)
        {
            iterableType = returnType;
            elementType = Types.findGenericParameter(iterableType, Iterable.class)
                    .orElseThrow(() -> new IllegalStateException(
                            "Cannot reflect Iterable<T> element type T in method return type " + returnType));
        }

        @Override
        @SuppressWarnings("unchecked")
        protected Object result(ResultBearing<?> q, Supplier<Handle> handle)
        {
            if (q instanceof Query) {
                Collector collector = ((Query) q).getContext().findCollectorFor(iterableType)
                        .orElseThrow(() -> new IllegalStateException("No collector available for " + iterableType));
                return q.collect(collector);
            } else {
                throw new UnsupportedOperationException("Collect is not supported for " + q);
            }
        }

        @Override
        protected Type elementType()
        {
            return elementType;
        }
    }
}
