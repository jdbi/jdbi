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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.internal.AnnotationFactory;
import org.jdbi.v3.meta.Beta;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toSet;

/**
 * A {@link java.lang.reflect.Type} qualified by a set of qualifier annotations. Two qualified types are equal to each other
 * if their {@link #getType()} and {@link #getQualifiers()} properties are equal.
 *
 * @param <T> the type that is qualified
 */
@Beta
public final class QualifiedType<T> {
    private final Type type;
    private final Set<Annotation> qualifiers;

    /**
     * Creates a {@code QualifiedType<T>} for a {@code Class<T>} with no qualifiers.
     * @param clazz the unqualified type
     * @return the unqualified QualifiedType
     * @see #with(Annotation...) to then qualify your type
     */
    public static <T> QualifiedType<T> of(Class<T> clazz) {
        return new QualifiedType<>(clazz, emptySet());
    }

    /**
     * Creates a wildcard {@code QualifiedType<?>} for a {@link Type} with no qualifiers.
     * @param type the unqualified type
     * @return the unqualified QualifiedType
     * @see #with(Annotation...) to then qualify your type
     */
    public static QualifiedType<?> of(Type type) {
        return new QualifiedType<>(type, emptySet());
    }

    /**
     * Creates a {@code QualifiedType<T>} for a {@code GenericType<T>} with no qualifiers.
     * @param type the unqualified type
     * @return the unqualified QualifiedType
     * @see #with(Annotation...) to then qualify your type
     */
    @SuppressWarnings("unchecked")
    public static <T> QualifiedType<T> of(GenericType<T> type) {
        return (QualifiedType<T>) of(type.getType());
    }

    private QualifiedType(Type type, Set<? extends Annotation> qualifiers) {
        this.type = type;
        this.qualifiers = unmodifiableSet(qualifiers);
    }

    /**
     * Returns a QualifiedType that has the same type as this instance, but with <b>only</b> the given qualifiers.
     *
     * @param qualifiers the qualifiers for the new qualified type.
     * @return the QualifiedType
     */
    public QualifiedType<T> with(Annotation... qualifiers) {
        return new QualifiedType<>(type, Arrays.stream(qualifiers).collect(toSet()));
    }

    /**
     * Returns a QualifiedType that has the same type as this instance, but with <b>only</b> the given qualifiers.
     *
     * @param qualifiers the qualifiers for the new qualified type.
     * @throws IllegalArgumentException if any of the given qualifier types have annotation attributes.
     * @return the QualifiedType
     */
    @SafeVarargs
    public final QualifiedType<T> with(Class<? extends Annotation>... qualifiers) {
        Set<? extends Annotation> annotations = Arrays.stream(qualifiers)
            .map(AnnotationFactory::create)
            .collect(toSet());
        return new QualifiedType<>(type, annotations);
    }

    /**
     * @return a QualifiedType that has the same type as this instance, but with <b>only</b> the given qualifiers.
     *
     * @param qualifiers the qualifiers for the new qualified type.
     */
    public QualifiedType<T> withAnnotations(Iterable<? extends Annotation> qualifiers) {
        return new QualifiedType<>(type, StreamSupport.stream(qualifiers.spliterator(), false).collect(toSet()));
    }

    /**
     * @return a QualifiedType that has the same type as this instance, but with <b>only</b> the given qualifiers.
     *
     * @param qualifiers the qualifiers for the new qualified type.
     */
    public QualifiedType<T> withAnnotationClasses(Iterable<Class<? extends Annotation>> qualifiers) {
        Set<? extends Annotation> annotations = StreamSupport.stream(qualifiers.spliterator(), false)
            .map(AnnotationFactory::create)
            .collect(toSet());
        return new QualifiedType<>(type, annotations);
    }

    /**
     * @return the type being qualified
     */
    public Type getType() {
        return type;
    }

    /**
     * @return the type qualifiers.
     */
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    /**
     * Apply the provided mapping function to the type, and if non-empty is returned,
     * return an {@code Optional<QualifiedType<?>>} with the returned type, and the same
     * qualifiers as this instance.
     *
     * @param mapper a mapping function to apply to the type
     * @return an optional qualified type with the mapped type and the same qualifiers
     */
    public Optional<QualifiedType<?>> mapType(Function<Type, Optional<Type>> mapper) {
        return mapper.apply(type).map(mappedType -> new QualifiedType<>(mappedType, qualifiers));
    }

    /**
     * @param qualifier qualifier to check for
     * @return true if this instance contains the given qualifier
     */
    public boolean hasQualifier(Class<? extends Annotation> qualifier) {
        return qualifiers.stream().anyMatch(qualifier::isInstance);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QualifiedType<?> that = (QualifiedType<?>) o;
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
