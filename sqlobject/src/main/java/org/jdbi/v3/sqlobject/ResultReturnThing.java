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

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeBindings;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.ResolvedMethod;

import org.jdbi.v3.Query;
import org.jdbi.v3.ResultBearing;
import org.jdbi.v3.ResultIterator;
import org.jdbi.v3.exceptions.UnableToCreateStatementException;
import org.jdbi.v3.sqlobject.customizers.Mapper;
import org.jdbi.v3.sqlobject.customizers.SingleValueResult;
import org.jdbi.v3.tweak.ResultSetMapper;

abstract class ResultReturnThing
{
    public Object map(ResolvedMethod method, Query<?> q, HandleDing h)
    {
        if (method.getRawMember().isAnnotationPresent(Mapper.class)) {
            final ResultSetMapper<?> mapper;
            try {
                mapper = method.getRawMember().getAnnotation(Mapper.class).value().newInstance();
            }
            catch (Exception e) {
                throw new UnableToCreateStatementException("unable to access mapper", e, null);
            }
            return result(q.map(mapper), h);
        }
        else {
            return result(q.mapTo(mapTo(method)), h);
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
        } else if (return_type.isInstanceOf(ResultBearing.class)) {
            return new ResultBearingResultReturnThing(method);
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

    protected abstract Object result(ResultBearing<?> q, HandleDing baton);

    protected abstract ResolvedType mapTo(ResolvedMethod method);


    static class SingleValueResultReturnThing extends ResultReturnThing
    {
        private final ResolvedType returnType;
        private final ResolvedType containerType;

        SingleValueResultReturnThing(ResolvedMethod method)
        {
            if (method.getRawMember().isAnnotationPresent(SingleValueResult.class)) {
                SingleValueResult svr = method.getRawMember().getAnnotation(SingleValueResult.class);
                // try to guess generic type
                if(SingleValueResult.Default.class == svr.value()){
                    TypeBindings typeBindings = method.getReturnType().getTypeBindings();
                    if(typeBindings.size() == 1){
                        this.returnType = typeBindings.getBoundType(0);
                    }else{
                        throw new IllegalArgumentException("Ambiguous generic information. SingleValueResult type could not be fetched.");
                    }

                }else{
                    this.returnType = new TypeResolver().resolve(svr.value());
                }
                this.containerType = method.getReturnType();
            }
            else {
                this.returnType = method.getReturnType();
                this.containerType = null;
            }

        }

        @Override
        protected Object result(ResultBearing<?> q, HandleDing baton)
        {
            if (containerType != null) {
                return q.collectInto(new TypeResolver().resolve(containerType));
            }
            return q.findFirst().orElse(null);
        }

        @Override
        protected ResolvedType mapTo(ResolvedMethod method)
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
        protected Object result(ResultBearing<?> q, HandleDing baton)
        {
            return q;
        }

        @Override
        protected ResolvedType mapTo(ResolvedMethod method)
        {
            return resolvedType;
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
        protected Object result(ResultBearing<?> q, final HandleDing baton)
        {
            final ResultIterator<?> itty = q.iterator();

            final boolean isEmpty = !itty.hasNext();
            if (isEmpty) {
                itty.close();
            } else {
                baton.retain("iterator");
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
                        try {
                            itty.close();
                        }
                        finally {
                            baton.release("iterator");
                        }
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
        protected ResolvedType mapTo(ResolvedMethod method)
        {
            return resolvedType;
        }
    }

    static class IterableReturningThing extends ResultReturnThing
    {
        private final ResolvedType iterableType;
        private final ResolvedType elementType;

        IterableReturningThing(ResolvedMethod method)
        {
            // extract T from List<T>
            this.iterableType = method.getReturnType();
            this.elementType = iterableType.typeParametersFor(Iterable.class).get(0);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected Object result(ResultBearing<?> q, HandleDing baton)
        {
            if (q instanceof Query) {
                return q.collectInto(iterableType);
            } else {
                throw new UnsupportedOperationException("Collect is not supported for " + q);
            }
        }

        @Override
        protected ResolvedType mapTo(ResolvedMethod method)
        {
            return elementType;
        }
    }
}
