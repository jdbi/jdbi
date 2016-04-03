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

import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeBindings;
import com.fasterxml.classmate.members.ResolvedMethod;

import org.jdbi.v3.Handle;
import org.jdbi.v3.Query;
import org.jdbi.v3.ResultBearing;
import org.jdbi.v3.ResultIterator;
import org.jdbi.v3.exceptions.UnableToCreateStatementException;
import org.jdbi.v3.sqlobject.customizers.Mapper;
import org.jdbi.v3.sqlobject.customizers.SingleValueResult;
import org.jdbi.v3.tweak.ResultSetMapper;

abstract class ResultReturnThing
{
    public Object map(ResolvedMethod method, Query<?> q, Supplier<Handle> handle)
    {
        if (method.getRawMember().isAnnotationPresent(Mapper.class)) {
            final ResultSetMapper<?> mapper;
            try {
                mapper = method.getRawMember().getAnnotation(Mapper.class).value().newInstance();
            }
            catch (Exception e) {
                throw new UnableToCreateStatementException("unable to access mapper", e, null);
            }
            return result(q.map(mapper), handle);
        }
        else {
            return result(q.mapTo(mapTo(method)), handle);
        }
    }

    static ResultReturnThing forType(ResolvedMethod method)
    {
        ResolvedType return_type = method.getReturnType();
        if (return_type == null) {
            throw new IllegalStateException(String.format(
                    "Method %s#%s is annotated as if it should return a value, but the method is void.",
                    method.getDeclaringType().getErasedType().getName(),
                    method.getName()));
        }
        else if (return_type.isInstanceOf(ResultBearing.class)) {
            return new ResultBearingResultReturnThing(method);
        }
        else if (return_type.isInstanceOf(Stream.class)) {
            return new StreamReturnThing(method);
        }
        else if (return_type.isInstanceOf(Iterable.class)) {
            return new IterableReturningThing(method);
        }
        else if (return_type.isInstanceOf(Iterator.class)) {
            return new IteratorResultReturnThing(method);
        }
        else {
            return new SingleValueResultReturnThing(method);
        }
    }

    protected abstract Object result(ResultBearing<?> q, Supplier<Handle> handle);

    protected abstract Class<?> mapTo(ResolvedMethod method);

    static class StreamReturnThing extends ResultReturnThing
    {
        private final ResolvedType type;

        StreamReturnThing(ResolvedMethod method)
        {
            type = method.getReturnType().typeParametersFor(Stream.class).get(0);
        }

        @Override
        protected Object result(ResultBearing<?> q, Supplier<Handle> handle) {
            return q.stream();
        }

        @Override
        protected Class<?> mapTo(ResolvedMethod method) {
            return type.getErasedType();
        }
    }

    static class SingleValueResultReturnThing extends ResultReturnThing
    {
        private final Class<?> returnType;
        private final Class<?> containerType;

        SingleValueResultReturnThing(ResolvedMethod method)
        {
            if (method.getRawMember().isAnnotationPresent(SingleValueResult.class)) {
                SingleValueResult svr = method.getRawMember().getAnnotation(SingleValueResult.class);
                // try to guess generic type
                if(SingleValueResult.Default.class == svr.value()){
                    TypeBindings typeBindings = method.getReturnType().getTypeBindings();
                    if(typeBindings.size() == 1){
                        this.returnType = typeBindings.getBoundType(0).getErasedType();
                    }else{
                        throw new IllegalArgumentException("Ambiguous generic information. SingleValueResult type could not be fetched.");
                    }

                }else{
                    this.returnType = svr.value();
                }
                this.containerType = method.getReturnType().getErasedType();
            }
            else {
                this.returnType = method.getReturnType().getErasedType();
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
        protected Class<?> mapTo(ResolvedMethod method)
        {
            return returnType;
        }
    }

    static class ResultBearingResultReturnThing extends ResultReturnThing
    {

        private final ResolvedType resolvedType;

        ResultBearingResultReturnThing(ResolvedMethod method)
        {
            // extract T from Query<T>
            ResolvedType query_type = method.getReturnType();
            List<ResolvedType> query_return_types = query_type.typeParametersFor(org.jdbi.v3.Query.class);
            this.resolvedType = query_return_types.get(0);
        }

        @Override
        protected Object result(ResultBearing<?> q, Supplier<Handle> handle)
        {
            return q;
        }

        @Override
        protected Class<?> mapTo(ResolvedMethod method)
        {
            return resolvedType.getErasedType();
        }
    }

    static class IteratorResultReturnThing extends ResultReturnThing
    {
        private final ResolvedType resolvedType;

        IteratorResultReturnThing(ResolvedMethod method)
        {
            ResolvedType query_type = method.getReturnType();
            List<ResolvedType> query_return_types = query_type.typeParametersFor(Iterator.class);
            this.resolvedType = query_return_types.get(0);

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
        protected Class<?> mapTo(ResolvedMethod method)
        {
            return resolvedType.getErasedType();
        }
    }

    static class IterableReturningThing extends ResultReturnThing
    {
        private final Class<?> iterableType;
        private final Class<?> elementType;

        IterableReturningThing(ResolvedMethod method)
        {
            // extract T from List<T>
            ResolvedType returnType = method.getReturnType();
            this.iterableType = returnType.getErasedType();
            this.elementType = returnType.typeParametersFor(Iterable.class).get(0).getErasedType();
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
        protected Class<?> mapTo(ResolvedMethod method)
        {
            return elementType;
        }
    }
}
