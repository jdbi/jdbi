/*
 * Copyright (C) 2004 - 2013 Brian McCallister
 *
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
package org.skife.jdbi.v3.sqlobject;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeBindings;
import com.fasterxml.classmate.members.ResolvedMethod;

import org.skife.jdbi.v3.Query;
import org.skife.jdbi.v3.ResultBearing;
import org.skife.jdbi.v3.ResultIterator;
import org.skife.jdbi.v3.exceptions.UnableToCreateStatementException;
import org.skife.jdbi.v3.sqlobject.customizers.Mapper;
import org.skife.jdbi.v3.sqlobject.customizers.SingleValueResult;
import org.skife.jdbi.v3.tweak.ResultSetMapper;

import java.util.Iterator;
import java.util.List;

abstract class ResultReturnThing
{
    public Object map(ResolvedMethod method, Query q, HandleDing h)
    {
        if (method.getRawMember().isAnnotationPresent(Mapper.class)) {
            final ResultSetMapper mapper;
            try {
                mapper = method.getRawMember().getAnnotation(Mapper.class).value().newInstance();
            }
            catch (Exception e) {
                throw new UnableToCreateStatementException("unable to access mapper", e);
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

    protected abstract Object result(ResultBearing q, HandleDing baton);

    protected abstract Class<?> mapTo(ResolvedMethod method);


    static class SingleValueResultReturnThing extends ResultReturnThing
    {
        private final Class<?> returnType;
        private final Class<?> containerType;

        public SingleValueResultReturnThing(ResolvedMethod method)
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
        protected Object result(ResultBearing q, HandleDing baton)
        {
            return null == containerType ? q.first() : q.first(containerType);
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

        public ResultBearingResultReturnThing(ResolvedMethod method)
        {
            // extract T from Query<T>
            ResolvedType query_type = method.getReturnType();
            List<ResolvedType> query_return_types = query_type.typeParametersFor(org.skife.jdbi.v3.Query.class);
            this.resolvedType = query_return_types.get(0);

        }

        @Override
        protected Object result(ResultBearing q, HandleDing baton)
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

        public IteratorResultReturnThing(ResolvedMethod method)
        {
            ResolvedType query_type = method.getReturnType();
            List<ResolvedType> query_return_types = query_type.typeParametersFor(Iterator.class);
            this.resolvedType = query_return_types.get(0);

        }

        @Override
        protected Object result(ResultBearing q, final HandleDing baton)
        {
            baton.retain("iterator");
            final ResultIterator itty = q.iterator();

            return new ResultIterator()
            {
                public void close()
                {
                    itty.close();
                }

                public boolean hasNext()
                {
                    boolean has_next = itty.hasNext();
                    if (!has_next) {
                        baton.release("iterator");
                    }
                    return itty.hasNext();
                }

                public Object next()
                {
                    Object rs = itty.next();
                    boolean has_next = itty.hasNext();
                    if (!has_next) {
                        baton.release("iterator");
                    }
                    return rs;
                }

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
        private final ResolvedType resolvedType;
        private final Class<?> erased_type;

        public IterableReturningThing(ResolvedMethod method)
        {
            // extract T from List<T>
            ResolvedType query_type = method.getReturnType();
            List<ResolvedType> query_return_types = query_type.typeParametersFor(Iterable.class);
            this.resolvedType = query_return_types.get(0);
            erased_type = method.getReturnType().getErasedType();
        }

        @Override
        protected Object result(ResultBearing q, HandleDing baton)
        {
            return q.list(erased_type);
        }

        @Override
        protected Class<?> mapTo(ResolvedMethod method)
        {
            return resolvedType.getErasedType();
        }
    }
}
