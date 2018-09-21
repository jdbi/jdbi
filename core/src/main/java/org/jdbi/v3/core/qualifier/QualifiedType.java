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
package org.jdbi.v3.core.qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.meta.Beta;

/**
 * A {@link java.lang.reflect.Type} qualified by a set of qualifier objects. Two qualified types are equal to each other
 * if their {@link #getType()} and {@link #getQualifiers()} properties are equal.
 */
@Beta
public final class QualifiedType {
    private final Type type;
    private final Set<Annotation> qualifiers;

    /**
     * Creates a QualifiedType for {@code type} with no qualifiers. In practice, the returned object is treated the same
     * as using {@code type} raw.
     */
    public static QualifiedType of(Type type) {
        return new QualifiedType(
            type,
            Collections.emptySet());
    }

    /**
     * Creates a QualifiedType for {@code type} with the given qualifier.
     */
    public static QualifiedType of(Type type, Annotation qualifier) {
        return new QualifiedType(
            type,
            Collections.singleton(qualifier));
    }

    /**
     * Creates a QualifiedType for {@code type} with the given qualifiers.
     */
    public static QualifiedType of(Type type, Annotation... qualifiers) {
        return new QualifiedType(
            type,
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(qualifiers))));
    }

    /**
     * Creates a QualifiedType for {@code type} with the given qualifiers.
     */
    public static QualifiedType of(Type type, Set<? extends Annotation> qualifiers) {
        return new QualifiedType(
            type,
            Collections.unmodifiableSet(new HashSet<>(qualifiers)));
    }

    /**
     * Creates a QualifiedType for {@code type} with no qualifiers. In practice, the returned object is treated the same
     * as using {@code type} raw.
     */
    public static QualifiedType of(GenericType<?> type) {
        return QualifiedType.of(type.getType());
    }

    /**
     * Creates a QualifiedType for {@code type} with the given qualifier.
     */
    public static QualifiedType of(GenericType<?> type, Annotation qualifier) {
        return QualifiedType.of(type.getType(), qualifier);
    }

    /**
     * Creates a QualifiedType for {@code type} with the given qualifiers.
     */
    public static QualifiedType of(GenericType<?> type, Annotation... qualifiers) {
        return QualifiedType.of(type.getType(), qualifiers);
    }

    /**
     * Creates a QualifiedType for {@code type} with the given qualifiers.
     */
    public static QualifiedType of(GenericType<?> type, Set<? extends Annotation> qualifiers) {
        return QualifiedType.of(type.getType(), qualifiers);
    }

    private QualifiedType(Type type, Set<Annotation> qualifiers) {
        this.type = type;
        this.qualifiers = qualifiers;
    }

    /**
     * Returns the type being qualified
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the type qualifiers.
     */
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    /**
     * Apply the provided mapping function to the type, and if non-empty is returned,
     * return an {@code Optional<QualifiedType>} with the returned type, and the same
     * qualifiers as this instance.
     *
     * @param mapper a mapping function to apply to the type
     * @return an optional qualified type with the mapped type and the same qualifiers
     */
    public Optional<QualifiedType> mapType(Function<Type, Optional<Type>> mapper) {
        return mapper.apply(type).map(mappedType -> new QualifiedType(mappedType, qualifiers));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QualifiedType that = (QualifiedType) o;
        return Objects.equals(type, that.type)
            && Objects.equals(qualifiers, that.qualifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, qualifiers);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        qualifiers.forEach(qualifier -> builder.append(qualifier).append(" "));
        builder.append(type.getTypeName());
        return builder.toString();
    }
}
