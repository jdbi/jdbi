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

package com.nebhale.r2dbc.postgresql.message.frontend;

/**
 * An enumeration of execution types as used by {@link FrontendMessage}s.
 */
public enum ExecutionType {

    /**
     * The Portal execution type, represented by the {@code P} character.
     */
    PORTAL('P'),

    /**
     * The Statement execution type, represented by the {@code S} character.
     */
    STATEMENT('S');

    private final char discriminator;

    ExecutionType(char discriminator) {
        this.discriminator = discriminator;
    }

    char getDiscriminator() {
        return this.discriminator;
    }

}
