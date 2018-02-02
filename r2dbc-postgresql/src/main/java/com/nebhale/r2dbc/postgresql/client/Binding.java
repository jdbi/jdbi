/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nebhale.r2dbc.postgresql.client;

import com.nebhale.r2dbc.postgresql.message.Format;
import io.netty.buffer.ByteBuf;

import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * A collection of {@link Parameter}s for a single bind invocation of an {@link ExtendedQueryMessageFlow}.
 */
public final class Binding {

    private final Parameter defaultParameter;

    private final SortedMap<Integer, Parameter> parameters = new TreeMap<>();

    /**
     * Creates a new {@link Binding}.
     *
     * @param defaultParameter the parameter to use as a default if otherwise unspecified
     * @throws NullPointerException if {@code defaultParameter} is {@code null}
     */
    public Binding(Parameter defaultParameter) {
        this.defaultParameter = Objects.requireNonNull(defaultParameter, "defaultParameter must not be null");
    }

    /**
     * Add a {@link Parameter} to the binding.
     *
     * @param identifier the identifier of the {@link Parameter}
     * @param parameter  the {@link Parameter}
     * @return this {@link Binding}
     */
    public Binding add(Integer identifier, Parameter parameter) {
        requireNonNull(identifier, "identifier must not be null");
        requireNonNull(parameter, "parameter must not be null");

        this.parameters.put(identifier, parameter);

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Binding that = (Binding) o;
        return Objects.equals(this.parameters, that.parameters);
    }

    /**
     * Returns the formats of the parameters in the binding.
     *
     * @return the formats of the parameters in the binding
     */
    public List<Format> getParameterFormats() {
        return getParameters()
            .map(Parameter::getFormat)
            .collect(Collectors.toList());
    }

    /**
     * Returns the types of the parameters in the binding.
     *
     * @return the types of the parameters in the binding
     */
    public List<Integer> getParameterTypes() {
        return getParameters()
            .map(Parameter::getType)
            .collect(Collectors.toList());
    }

    /**
     * Returns the values of the parameters in the binding.
     *
     * @return the values of the parameters in the binding
     */
    public List<ByteBuf> getParameterValues() {
        return getParameters()
            .map(Parameter::getValue)
            .collect(Collectors.toList());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.parameters);
    }

    @Override
    public String toString() {
        return "Binding{" +
            "parameters=" + this.parameters +
            '}';
    }

    private Parameter get(Integer identifier) {
        return this.parameters.getOrDefault(identifier, this.defaultParameter);
    }

    private Stream<Parameter> getParameters() {
        return IntStream.range(0, size())
            .mapToObj(this::get);
    }

    private int size() {
        return this.parameters.isEmpty() ? 0 : this.parameters.lastKey() + 1;
    }

}
