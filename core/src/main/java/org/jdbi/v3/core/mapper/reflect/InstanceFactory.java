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
package org.jdbi.v3.core.mapper.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableList;

abstract class InstanceFactory<T> {
    private final Executable executable;

    protected InstanceFactory(Executable executable) {
        this.executable = requireNonNull(executable, "executable is null");
    }

    Class<?> getDeclaringClass() {
        return executable.getDeclaringClass();
    }

    int getParameterCount() {
        return executable.getParameterCount();
    }

    Parameter[] getParameters() {
        return executable.getParameters();
    }

    List<Type> getTypes() {
        return Arrays.stream(getParameters())
            .map(Parameter::getParameterizedType)
            .collect(toUnmodifiableList());
    }

    @Nullable
    <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return executable.getAnnotation(annotationClass);
    }

    @Nullable
    <A extends Annotation> A getAnnotationIncludingType(Class<A> annotationClass) {
        return Stream.of(
                getAnnotation(annotationClass),
                executable.getDeclaringClass().getAnnotation(annotationClass))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    abstract T newInstance(Object... params);

    @Override
    public abstract String toString();
}
