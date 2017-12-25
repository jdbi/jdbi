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
import reactor.core.publisher.Flux;

/**
 * A decoder that reads {@link ByteBuf}s and returns a {@link Flux} of decoded {@link BackendMessage}s.
 */
public interface BackendMessageDecoder {

    /**
     * Decode a message from a {@link ByteBuf}.
     *
     * @param in the {@link ByteBuf} to read from
     * @return a {@link Flux} that produces the {@link BackendMessage}s contained in the encoded message
     */
    Flux<BackendMessage> decode(ByteBuf in);

}
