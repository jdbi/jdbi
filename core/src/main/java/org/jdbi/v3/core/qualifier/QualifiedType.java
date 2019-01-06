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

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.internal.AnnotationFactory;
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
        return new QualifiedType(type, emptySet());
    }

    /**
     * Creates a QualifiedType for {@code type} with no qualifiers. In practice, the returned object is treated the same
     * as using {@code type} raw.
     */
    public static QualifiedType of(GenericType<?> type) {
        return of(type.getType());
    }

    private QualifiedType(Type type, Set<Annotation> qualifiers) {
        this.type = type;
        this.qualifiers = qualifiers;
    }

    /**
     * Returns a QualifiedType that has the same type as this instance, but with the given qualifiers.
     *
     * @param qualifiers the qualifiers for the new qualified type.
     */
    public QualifiedType with(Annotation... qualifiers) {
        return with(Arrays.asList(qualifiers));
    }

    /**
     * Returns a QualifiedType that has the same type as this instance, but with the given qualifiers.
     *
     * @param qualifiers the qualifiers for the new qualified type.
     * @throws IllegalArgumentException if any of the given qualifier types have annotation attributes.
     */
    @SafeVarargs
    public final QualifiedType with(Class<? extends Annotation>... qualifiers) {
        return with(Arrays.stream(qualifiers).map(AnnotationFactory::create).collect(toList()));
    }

    /**
     * Returns a QualifiedType that has the same type as this instance, but with the given qualifiers.
     *
     * @param qualifiers the qualifiers for the new qualified type.
     */
    public QualifiedType with(Collection<? extends Annotation> qualifiers) {
        return new QualifiedType(type, Collections.unmodifiableSet(new HashSet<>(qualifiers)));
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
