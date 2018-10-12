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
package org.jdbi.v3.core.mapper.reflect.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.qualifier.QualifiedType;

/**
 * {@link java.beans.Introspector}-like interface that works with arbitrary pojos, not just beans.
 */
public abstract class PojoProperties<T> {
    private final Type type;

    protected PojoProperties(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public abstract Map<String, ? extends PojoProperty<T>> getProperties();
    public abstract PojoBuilder<T> create();

    public interface PojoBuilder<T> {
        void set(String property, Object value);

        default void set(PojoProperty<T> property, Object value) {
            set(property.getName(), value);
        }

        T build();
    }

    public interface PojoProperty<T> {
        String getName();
        QualifiedType getQualifiedType();
        <A extends Annotation> Optional<A> getAnnotation(Class<A> anno);
        Object get(T pojo);
    }
}
