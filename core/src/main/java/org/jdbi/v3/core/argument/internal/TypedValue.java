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
package org.jdbi.v3.core.argument.internal;

import java.util.Objects;

import org.jdbi.v3.core.qualifier.QualifiedType;

import static java.util.Objects.requireNonNull;

/**
 * A container to combine a value with a specific type.
 */
public final class TypedValue {
    private final QualifiedType<?> type;
    private final Object value;

    public TypedValue(QualifiedType<?> qualifiedType, Object value) {
        this.type = requireNonNull(qualifiedType, "qualifiedType is null");
        this.value = value;
    }

    public QualifiedType<?> getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "(" + type + ") " + value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TypedValue that = (TypedValue) o;
        return type.equals(that.type) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }
}
