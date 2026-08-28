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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.internal.AnnotationFactory;

import static java.util.Collections.emptySet;

/**
 * A {@link java.lang.reflect.Type} qualified by a set of qualifier annotations. Two qualified types are equal to each other
 * if their {@link #getType()} and {@link #getQualifiers()} properties are equal.
 *
 * @param <T> the type that is qualified
 */
public final class QualifiedType<T> {
    private final Type type;
    private final Set<QualifierKey> qualifiers;
    private Set<Annotation> annotations;
    private int hashCode;

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

    private QualifiedType(Type type, Set<QualifierKey> qualifiers) {
        this.type = type;
        this.qualifiers = qualifiers;
    }

    /**
     * Returns a QualifiedType that has the same type as this instance, but with <b>only</b> the given qualifiers.
     *
     * @param newQualifiers the qualifiers for the new qualified type.
     * @return the QualifiedType
     */
    public QualifiedType<T> with(Annotation... newQualifiers) {
        return new QualifiedType<>(type, keysOf(Arrays.asList(newQualifiers)));
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
        return new QualifiedType<>(type, keysOfClasses(Arrays.asList(newQualifiers)));
    }

    /**
     * Creates a QualifiedType with the same type as this instance and new qualifiers. Old qualifiers are discarded.
     *
     * @return a QualifiedType that has the same type as this instance, but with <b>only</b> the given qualifiers.
     *
     * @param newQualifiers the qualifiers for the new qualified type.
     */
    public QualifiedType<T> withAnnotations(Iterable<? extends Annotation> newQualifiers) {
        return new QualifiedType<>(type, keysOf(newQualifiers));
    }

    /**
     * Creates a QualifiedType with the same type as this instance and new qualifiers. Old qualifiers are discarded.
     *
     * @return a QualifiedType that has the same type as this instance, but with <b>only</b> the given qualifiers.
     *
     * @param newQualifiers the qualifiers for the new qualified type.
     */
    public QualifiedType<T> withAnnotationClasses(Iterable<Class<? extends Annotation>> newQualifiers) {
        return new QualifiedType<>(type, keysOfClasses(newQualifiers));
    }

    private static Set<QualifierKey> keysOf(Iterable<? extends Annotation> annotations) {
        List<QualifierKey> keys = new ArrayList<>();
        for (Annotation annotation : annotations) {
            keys.add(QualifierKey.of(annotation));
        }
        return Set.copyOf(keys);
    }

    private static Set<QualifierKey> keysOfClasses(Iterable<Class<? extends Annotation>> annotationTypes) {
        List<QualifierKey> keys = new ArrayList<>();
        for (Class<? extends Annotation> annotationType : annotationTypes) {
            keys.add(QualifierKey.of(annotationType));
        }
        return Set.copyOf(keys);
    }

    /**
     * Returns the qualified type.
     *
     * @return the type being qualified.
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns a set of qualifying annotations. Qualifiers that were given as annotation classes rather than
     * annotation instances are synthesized on first call.
     *
     * @return the type qualifiers.
     */
    public Set<Annotation> getQualifiers() {
        Set<Annotation> result = annotations;
        if (result == null) {
            List<Annotation> collected = new ArrayList<>();
            for (QualifierKey key : qualifiers) {
                collected.add(key.annotation());
            }
            result = Set.copyOf(collected);
            annotations = result;
        }
        return result;
    }

    /**
     * Returns true if the qualifiers of this type are exactly the given annotations.
     *
     * @param annotations the annotations to compare against.
     * @return true if this instance has exactly the given qualifiers.
     */
    public boolean hasQualifiers(Set<? extends Annotation> annotations) {
        if (qualifiers.size() != annotations.size()) {
            return false;
        }
        for (Annotation annotation : annotations) {
            if (!qualifiers.contains(QualifierKey.of(annotation))) {
                return false;
            }
        }
        return true;
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
     * Returns true if this type contains the given qualifier.
     *
     * @param qualifier qualifier to check for.
     * @return true if this instance contains the given qualifier.
     */
    public boolean hasQualifier(Class<? extends Annotation> qualifier) {
        for (QualifierKey key : qualifiers) {
            if (key.type() == qualifier) {
                return true;
            }
        }
        return false;
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
            h = Objects.hash(type, qualifiers);
            hashCode = h;
        }
        return h;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        qualifiers.forEach(qualifier -> builder.append(qualifier).append(' '));
        builder.append(type.getTypeName());
        return builder.toString();
    }

    /**
     * Identity of one qualifier. A qualifier annotation that declares no members is identified by its type alone,
     * so it never needs an annotation instance. A qualifier with members is identified by an instance, and relies
     * on the {@link Annotation#equals(Object)} contract.
     */
    private record QualifierKey(Class<? extends Annotation> type, Annotation instance) {
        private static final ClassValue<Boolean> HAS_MEMBERS = new ClassValue<>() {
            @Override
            protected Boolean computeValue(Class<?> annotationType) {
                return annotationType.getDeclaredMethods().length > 0;
            }
        };

        static QualifierKey of(Annotation annotation) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            return new QualifierKey(annotationType, HAS_MEMBERS.get(annotationType) ? annotation : null);
        }

        static QualifierKey of(Class<? extends Annotation> annotationType) {
            return new QualifierKey(annotationType, HAS_MEMBERS.get(annotationType) ? AnnotationFactory.create(annotationType) : null);
        }

        Annotation annotation() {
            return instance != null ? instance : AnnotationFactory.create(type);
        }

        @Override
        public String toString() {
            return instance != null ? instance.toString() : "@" + type.getName() + "()";
        }
    }
}
