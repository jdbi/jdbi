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

package com.nebhale.r2dbc.postgresql.message.backend;

import io.netty.buffer.ByteBuf;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The ParameterDescription message.
 */
public final class ParameterDescription implements BackendMessage {

    private final List<Integer> parameters;

    /**
     * Creates a new message.
     *
     * @param parameters the object IDs of the parameter data types
     * @throws NullPointerException if {@code parameters} is {@code null}
     */
    public ParameterDescription(List<Integer> parameters) {
        this.parameters = Objects.requireNonNull(parameters, "parameters must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParameterDescription that = (ParameterDescription) o;
        return Objects.equals(this.parameters, that.parameters);
    }

    /**
     * Returns the object IDs of the parameter data types.
     *
     * @return the object IDs of the parameter data types
     */
    public List<Integer> getParameters() {
        return this.parameters;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.parameters);
    }

    @Override
    public String toString() {
        return "ParameterDescription{" +
            "parameters=" + this.parameters +
            '}';
    }

    static ParameterDescription decode(ByteBuf in) {
        Objects.requireNonNull(in, "in must not be null");

        List<Integer> parameters = IntStream.range(0, in.readShort())
            .map(i -> in.readInt())
            .boxed()
            .collect(Collectors.toList());

        return new ParameterDescription(parameters);
    }

}
