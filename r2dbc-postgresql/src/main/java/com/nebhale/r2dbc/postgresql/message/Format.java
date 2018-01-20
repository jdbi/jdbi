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

package com.nebhale.r2dbc.postgresql.message;

import com.nebhale.r2dbc.postgresql.message.backend.BackendMessage;
import com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessage;

import java.util.Arrays;

/**
 * An enumeration of argument formats as used by {@link BackendMessage}s and {@link FrontendMessage}s.
 */
public enum Format {

    /**
     * The binary data format, represented by the {@code 1} byte.
     */
    BINARY((byte) 1),

    /**
     * The text data format, represented by the {@code 0} byte.
     */
    TEXT((byte) 0);

    private final byte discriminator;

    Format(byte discriminator) {
        this.discriminator = discriminator;
    }

    /**
     * Returns the enum constant of this type with the specified discriminator.
     *
     * @param s the discriminator
     * @return the enum constant of this type with the specified discriminator
     */
    public static Format valueOf(short s) {
        return Arrays.stream(values())
            .filter(type -> type.discriminator == s)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(String.format("%d is not a valid format", s)));
    }

    /**
     * Returns the discriminator for the format.
     *
     * @return the discriminator for the format
     */
    public byte getDiscriminator() {
        return this.discriminator;
    }

    @Override
    public String toString() {
        return "Format{" +
            "discriminator=" + this.discriminator +
            "} " + super.toString();
    }

}
