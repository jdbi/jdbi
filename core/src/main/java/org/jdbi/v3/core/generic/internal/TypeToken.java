/*
 * Copyright (C) 2006 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.jdbi.v3.core.generic.internal;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Collections.singletonMap;
import static org.jdbi.v3.core.generic.internal.Preconditions.checkNotNull;
import static org.jdbi.v3.core.generic.internal.Preconditions.checkState;

public class TypeToken<T> extends TypeCapture<T> {

    private final Type runtimeType;

    private transient TypeResolver typeResolver;

    protected TypeToken() {
        this.runtimeType = capture();
        checkState(
                !(runtimeType instanceof TypeVariable),
                "Cannot construct a TypeToken for a type variable.\n"
                        + "You probably meant to call new TypeToken<%s>(getClass()) "
                        + "that can resolve the type variable for you.\n"
                        + "If you do need to create a TypeToken of a type variable, "
                        + "please use TypeToken.of() instead.",
                runtimeType);
    }

    private TypeToken(Type type) {
        this.runtimeType = checkNotNull(type, "type");
    }

    public static TypeToken<?> of(Type type) {
        return new SimpleTypeToken<>(type);
    }

    public final <X> TypeToken<T> where(TypeParameter<X> typeParam, TypeToken<X> typeArg) {
        TypeResolver resolver = new TypeResolver()
                        .where(singletonMap(new TypeResolver.TypeVariableKey(typeParam.typeVariable), typeArg.runtimeType));
        // If there's any type error, we'd report now rather than later.
        return new SimpleTypeToken<T>(resolver.resolveType(runtimeType));
    }

    public final TypeToken<?> resolveType(Type type) {
        checkNotNull(type, "type");
        TypeResolver resolver = typeResolver;
        if (resolver == null) {
            resolver = (typeResolver = TypeResolver.accordingTo(runtimeType));
        }
        return of(resolver.resolveType(type));
    }

    public final Type getType() {
        return runtimeType;
    }

    public final Class<? super T> getRawType() {
        // For wildcard or type variable, the first bound determines the runtime type.
        Class<?> rawType = getRawTypes().iterator().next();
        @SuppressWarnings("unchecked") // raw type is |T|
                Class<? super T> result = (Class<? super T>) rawType;
        return result;
    }

    private Set<Class<? super T>> getRawTypes() {
        final Set<Class<?>> builder = new LinkedHashSet<>();
        new AbstractTypeVisitor() {
            @Override
            void visitTypeVariable(TypeVariable<?> t) {
                visit(t.getBounds());
            }

            @Override
            void visitWildcardType(WildcardType t) {
                visit(t.getUpperBounds());
            }

            @Override
            void visitParameterizedType(ParameterizedType t) {
                builder.add((Class<?>) t.getRawType());
            }

            @Override
            void visitClass(Class<?> t) {
                builder.add(t);
            }

            @Override
            void visitGenericArrayType(GenericArrayType t) {
                builder.add(Types.getArrayClass(of(t.getGenericComponentType()).getRawType()));
            }
        }.visit(runtimeType);
        // Cast from ImmutableSet<Class<?>> to ImmutableSet<Class<? super T>>
        @SuppressWarnings({"unchecked", "rawtypes"})
        Set<Class<? super T>> result = (Set) Collections.unmodifiableSet(builder);
        return result;
    }

    private static final class SimpleTypeToken<T> extends TypeToken<T> {
        SimpleTypeToken(Type type) {
            super(type);
        }
    }
}
