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

package com.nebhale.r2dbc.postgresql.authentication;

import com.nebhale.r2dbc.core.nullability.Nullable;
import com.nebhale.r2dbc.postgresql.message.backend.AuthenticationMessage;
import com.nebhale.r2dbc.postgresql.message.frontend.FrontendMessage;

import java.util.Objects;

/**
 * An implementation of {@link AuthenticationHandler} that handles implicit trust authentication.
 */
public final class TrustAuthenticationHandler implements AuthenticationHandler {

    /**
     * A static singleton instance that should always be used.
     */
    public static final TrustAuthenticationHandler INSTANCE = new TrustAuthenticationHandler();

    private TrustAuthenticationHandler() {
    }

    @Override
    public FrontendMessage handle(@Nullable AuthenticationMessage message) {
        Objects.requireNonNull(message, "message must not be null");

        throw new IllegalArgumentException("Trust does not require a response");
    }

}
