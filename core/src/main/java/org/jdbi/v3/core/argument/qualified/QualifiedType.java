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
package org.jdbi.v3.core.argument.qualified;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class QualifiedType {
    private final Type type;
    private final Set<Object> qualifiers;

    public static QualifiedType of(Type type, Object qualifier) {
        return new QualifiedType(
            type,
            Collections.singleton(qualifier));
    }

    public static QualifiedType of(Type type, Object... qualifiers) {
        return new QualifiedType(
            type,
            Collections.unmodifiableSet(Stream.of(qualifiers).collect(Collectors.toSet())));
    }

    public static QualifiedType of(Type type, Set<?> qualifiers) {
        return new QualifiedType(
            type,
            Collections.unmodifiableSet(new HashSet<>(qualifiers)));
    }

    private QualifiedType(Type type, Set<Object> qualifiers) {
        this.type = type;
        this.qualifiers = qualifiers;
    }

    public Type getType() {
        return type;
    }

    public Set<Object> getQualifiers() {
        return qualifiers;
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
        return Objects.equals(type, that.type) &&
            Objects.equals(qualifiers, that.qualifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, qualifiers);
    }

    @Override
    public String toString() {
        return "QualifiedType{" +
            "type=" + type +
            ", qualifiers=" + qualifiers +
            '}';
    }
}
