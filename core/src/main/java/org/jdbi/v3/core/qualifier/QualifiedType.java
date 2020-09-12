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
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.internal.AnnotationFactory;
import org.jdbi.v3.meta.Beta;

/**
 * A {@link java.lang.reflect.Type} qualified by a set of qualifier annotations. Two qualified types are equal to each other
 * if their {@link #getType()} and {@link #getQualifiers()} properties are equal.
 *
 * @param <T> the type that is qualified
 */
@Beta
public final class QualifiedType<T> {
    private final Type type;
    // null for no qualifiers, Annotation itself for one qualifier, Set<Annotation> for many
    private final Object qualifiers;
    private int hashCode;

    /**
     * Creates a {@code QualifiedType<T>} for a {@code Class<T>} with no qualifiers.
     * @param clazz the unqualified type
     * @return the unqualified QualifiedType
     * @see #with(Annotation...) to then qualify your type
     */
    public static <T> QualifiedType<T> of(Class<T> clazz) {
        return new QualifiedType<>(clazz, null);
    }

    /**
     * Creates a wildcard {@code QualifiedType<?>} for a {@link Type} with no qualifiers.
     * @param type the unqualified type
     * @return the unqualified QualifiedType
     * @see #with(Annotation...) to then qualify your type
     */
    public static QualifiedType<?> of(Type type) {
        return new QualifiedType<>(type, null);
    }

    /**
     * Creates a wildcard {@code QualifiedType<?>} for a {@link Type} with the given qualifiers.
     * @param type the unqualified type
     * @param qualifiers the qualifiers for the type
     * @return the QualifiedType
     */
    public static <T> QualifiedType<T> of(Type type, Annotation[] qualifiers) {
        switch (qualifiers.length) {
        case 0:
            return new QualifiedType<>(type, null);
        case 1:
            return new QualifiedType<>(type, qualifiers[0]);
        default:
            Set<Annotation> set = new HashSet<>();
            for (Annotation qual : qualifiers) {
                set.add(qual);
            }
            if (set.size() == 1) {
                return new QualifiedType<>(type, qualifiers[0]);
            }
            return new QualifiedType<>(type, Collections.unmodifiableSet(set));
        }
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

    private QualifiedType(Type type, Object qualifiers) {
        this.type = type;
        this.qualifiers = qualifiers;
    }

    /**
     * Returns a QualifiedType that has the same type as this instance, but with <b>only</b> the given qualifier.
     *
     * @param newQualifier the qualifier for the new qualified type.
     * @return the QualifiedType
     */
    public QualifiedType<T> with(Annotation newQualifier) {
        return new QualifiedType<>(type, newQualifier);
    }

    /**
     * Returns a QualifiedType that has the same type as this instance, but with <b>only</b> the given qualifiers.
     *
     * @param newQualifiers the qualifiers for the new qualified type.
     * @return the QualifiedType
     */
    public QualifiedType<T> with(Annotation... newQualifiers) {
        return of(type, newQualifiers);
    }

    /**
     * Returns a QualifiedType that has the same type as this instance, but with <b>only</b> the given qualifiers.
     *
     * @param newQualifiers the qualifiers for the new qualified type.
     * @throws IllegalArgumentException if any of the given qualifier types have annotation attributes.
     * @return the QualifiedType
     */
    @SafeVarargs
    public final QualifiedType<T> with(Class<? extends Annotation>... newQualifiers) {
        return of(type, Arrays.stream(newQualifiers)
                .map(AnnotationFactory::create)
                .toArray(Annotation[]::new));
    }

    /**
     * @return a QualifiedType that has the same type as this instance, but with <b>only</b> the given qualifiers.
     *
     * @param newQualifiers the qualifiers for the new qualified type.
     */
    public QualifiedType<T> withAnnotations(Iterable<? extends Annotation> newQualifiers) {
        return of(type, StreamSupport.stream(newQualifiers.spliterator(), false)
                .toArray(Annotation[]::new));
    }

    /**
     * @return a QualifiedType that has the same type as this instance, but with <b>only</b> the given qualifiers.
     *
     * @param newQualifiers the qualifiers for the new qualified type.
     */
    public QualifiedType<T> withAnnotationClasses(Iterable<Class<? extends Annotation>> newQualifiers) {
        return of(type, StreamSupport.stream(newQualifiers.spliterator(), false)
            .map(AnnotationFactory::create)
            .toArray(Annotation[]::new));
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
    @SuppressWarnings("unchecked")
    public Set<Annotation> getQualifiers() {
        if (qualifiers == null) {
            return Collections.emptySet();
        } else if (qualifiers instanceof Set<?>) {
            return (Set<Annotation>) qualifiers;
        } else {
            return Collections.singleton((Annotation) qualifiers);
        }
    }

    public boolean qualifiersEqualTo(Set<Annotation> annotations) {
        if (qualifiers == null) {
            return annotations.isEmpty();
        }
        if (qualifiers instanceof Set<?>) {
            return annotations.equals(qualifiers);
        } else {
            Iterator<Annotation> iter = annotations.iterator();
            return iter.hasNext()
                    && iter.next().equals(qualifiers)
                    && !iter.hasNext();
        }
    }

    /**
     * Apply the provided mapping function to the type, and if non-empty is returned,
     * return an {@code Optional<QualifiedType<?>>} with the returned type, and the same
     * qualifiers as this instance.
     *
     * @param mapper a mapping function to apply to the type
     * @return an optional qualified type with the mapped type and the same qualifiers
     */
    public QualifiedType<?> mapType(Function<Type, Type> mapper) {
        return new QualifiedType<>(mapper.apply(type), qualifiers);
    }

    /**
     * Apply the provided mapping function to the type, and if non-empty is returned,
     * return an {@code Optional<QualifiedType<?>>} with the returned type, and the same
     * qualifiers as this instance.
     *
     * @param mapper a mapping function to apply to the type
     * @return an optional qualified type with the mapped type and the same qualifiers
     */
    public Optional<QualifiedType<?>> flatMapType(Function<Type, Optional<Type>> mapper) {
        return mapper.apply(type).map(mappedType -> new QualifiedType<>(mappedType, qualifiers));
    }

    /**
     * @param qualifier qualifier to check for
     * @return true if this instance contains the given qualifier
     */
    public boolean hasQualifier(Class<? extends Annotation> qualifier) {
        return getQualifiers().stream().anyMatch(qualifier::isInstance);
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
        int h = hashCode;
        if (h == 0) {
            hashCode = h = Objects.hash(type, qualifiers);
        }
        return h;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        getQualifiers().forEach(qualifier -> builder.append(qualifier).append(" "));
        builder.append(type.getTypeName());
        return builder.toString();
    }
}
