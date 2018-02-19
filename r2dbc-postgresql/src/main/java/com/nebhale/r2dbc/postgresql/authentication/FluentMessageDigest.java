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

import reactor.core.Exceptions;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static io.netty.util.CharsetUtil.UTF_8;
import static java.util.Objects.requireNonNull;

final class FluentMessageDigest {

    private final MessageDigest messageDigest;

    FluentMessageDigest(String algorithm) {
        requireNonNull(algorithm, "algorithm must not be null");

        try {
            this.messageDigest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw Exceptions.propagate(e);
        }
    }

    String digest() {
        return String.format("%032x", new BigInteger(1, this.messageDigest.digest()));
    }

    FluentMessageDigest update(String s) {
        requireNonNull(s, "s must not be null");

        this.messageDigest.update(s.getBytes(UTF_8));
        return this;
    }

    FluentMessageDigest update(String format, Object... args) {
        requireNonNull(format, "format must not be null");

        return update(String.format(format, args));
    }

    FluentMessageDigest update(ByteBuffer buffer) {
        requireNonNull(buffer, "buffer must not be null");

        this.messageDigest.update(buffer);
        return this;
    }

}
