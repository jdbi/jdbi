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

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.jdbi.v3.core.Query;
import org.jdbi.v3.core.ResultBearing;
import org.jdbi.v3.core.ResultIterable;
import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.exception.UnableToCreateStatementException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.util.GenericTypes;
import org.jdbi.v3.sqlobject.customizers.UseRowMapper;

abstract class ResultReturner
{
    public Object map(Method method, Query<?> q)
    {
        if (method.isAnnotationPresent(UseRowMapper.class)) {
            final RowMapper<?> mapper;
            try {
                mapper = method.getAnnotation(UseRowMapper.class).value().newInstance();
            }
            catch (Exception e) {
                throw new UnableToCreateStatementException("unable to access mapper", e, null);
            }
            return result(q.map(mapper));
        }
        else {
            return result(q.mapTo(elementType(q.getContext())));
        }
    }

    static ResultReturner forOptionalReturn(Class<?> extensionType, Method method)
    {
        if (method.getReturnType() == void.class) {
            return new ResultReturner() {
                @Override
                protected Object result(ResultIterable<?> bearer) {
                    bearer.stream().forEach(i -> {}); // Make sure to consume the result
                    return null;
                }
                @Override
                protected Type elementType(StatementContext ctx) {
                    return null;
                }
            };
        }
        return forMethod(extensionType, method);
    }

    static ResultReturner forMethod(Class<?> extensionType, Method method)
    {
        Type returnType = GenericTypes.resolveType(method.getGenericReturnType(), extensionType);
        Class<?> returnClass = GenericTypes.getErasedType(returnType);
        if (Void.TYPE.equals(returnClass)) {
            throw new IllegalStateException(String.format(
                    "Method %s#%s is annotated as if it should return a value, but the method is void.",
                    method.getDeclaringClass().getName(),
                    method.getName()));
        }
        else if (ResultBearing.class.isAssignableFrom(returnClass)) {
            return new ResultBearingResultReturner(returnType);
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
        else if (returnClass.isArray()) {
            return new ArrayResultReturner(returnClass.getComponentType());
        }
        else {
            return new DefaultResultReturner(returnType);
        }
    }

    protected abstract Object result(ResultIterable<?> bearer);

    static RowMapper<?> rowMapperFor(GetGeneratedKeys ggk, Type returnType) {
        if (DefaultGeneratedKeyMapper.class.equals(ggk.value())) {
            return new DefaultGeneratedKeyMapper(returnType, ggk.columnName());
        }
        else {
            try {
                return ggk.value().getConstructor().newInstance();
            }
            catch (Exception e) {
                throw new UnableToCreateStatementException("Unable to instantiate row mapper for statement", e, null);
            }
        }
    }

    protected abstract Type elementType(StatementContext ctx);

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
        protected Stream<?> result(ResultIterable<?> bearer) {
            return bearer.stream();
        }

        @Override
        protected Type elementType(StatementContext ctx) {
            return elementType;
        }
    }

    static class DefaultResultReturner extends ResultReturner
    {
        private final Type returnType;

        DefaultResultReturner(Type returnType)
        {
            this.returnType = returnType;
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        protected Object result(ResultIterable<?> bearer)
        {
            Collector collector = bearer.getContext().findCollectorFor(returnType).orElse(null);
            if (collector != null) {
                return bearer.collect(collector);
            }
            return bearer.findFirst().orElse(null);
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
        protected Object result(ResultIterable<?> bearer)
        {
            return bearer.findFirst().orElse(null);
        }

        @Override
        protected Type elementType(StatementContext ctx)
        {
            return returnType;
        }
    }

    static class ResultBearingResultReturner extends ResultReturner
    {

        private final Type elementType;

        ResultBearingResultReturner(Type returnType)
        {
            // extract T from Query<T>
            elementType = GenericTypes.findGenericParameter(returnType, Query.class)
                    .orElseThrow(() -> new IllegalStateException(
                            "Cannot reflect Query<T> element type T in method return type " + returnType));
        }

        @Override
        protected Object result(ResultIterable<?> bearer)
        {
            return bearer;
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
        protected Object result(ResultIterable<?> bearer)
        {
            return bearer.iterator();
        }

        @Override
        protected Type elementType(StatementContext ctx)
        {
            return elementType;
        }
    }

    static class ArrayResultReturner extends ResultReturner
    {
        private final Class<?> componentType;

        ArrayResultReturner(Class<?> componentType) {
            this.componentType = componentType;
        }

        @Override
        protected Object result(ResultIterable<?> bearer) {
            final List<?> list = bearer.list();
            Object result = Array.newInstance(componentType, list.size());
            for (int i = 0; i < list.size(); i++) {
                Array.set(result, i, list.get(i));
            }
            return result;
        }

        @Override
        protected Type elementType(StatementContext ctx) {
            return componentType;
        }
    }
}
