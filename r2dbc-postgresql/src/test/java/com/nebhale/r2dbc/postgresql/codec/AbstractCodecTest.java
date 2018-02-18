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

package com.nebhale.r2dbc.postgresql.codec;

import com.nebhale.r2dbc.postgresql.client.Parameter;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static com.nebhale.r2dbc.postgresql.message.Format.TEXT;
import static com.nebhale.r2dbc.postgresql.type.PostgresqlObjectId.INT4;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public final class AbstractCodecTest {

    @Test
    public void canEncode() {
        assertThat(new StubCodec<>(String.class).canEncode("")).isTrue();
        assertThat(new StubCodec<>(String.class).canEncode(new Object())).isFalse();
    }

    @Test
    public void constructorNoType() {
        assertThatNullPointerException().isThrownBy(() -> new StubCodec<>(null))
            .withMessage("type must not be null");
    }

    @Test
    public void create() {
        Parameter parameter = AbstractCodec.create(TEXT, INT4, Unpooled.buffer().writeInt(100));

        assertThat(parameter).isEqualTo(new Parameter(TEXT, INT4.getObjectId(), Unpooled.buffer().writeInt(100)));
    }

    @Test
    public void createNoFormat() {
        assertThatNullPointerException().isThrownBy(() -> new StubCodec<>(Object.class).create(null, INT4, null))
            .withMessage("format must not be null");
    }

    @Test
    public void createNoType() {
        assertThatNullPointerException().isThrownBy(() -> new StubCodec<>(Object.class).create(TEXT, null, null))
            .withMessage("type must not be null");
    }

    @Test
    public void encode() {
        Parameter parameter = new Parameter(TEXT, INT4.getObjectId(), Unpooled.buffer().writeInt(100));
        Object value = new Object();

        StubCodec<Object> codec = new StubCodec<>(Object.class, parameter);

        assertThat(codec.doEncode(value)).isSameAs(parameter);
        assertThat(codec.value).isSameAs(value);
    }

    @Test
    public void encodeNoValue() {
        assertThatNullPointerException().isThrownBy(() -> new StubCodec<>(Object.class).encode(null))
            .withMessage("value must not be null");
    }

    private static final class StubCodec<T> extends AbstractCodec<T> {

        private final Parameter parameter;

        private T value;

        StubCodec(Class<T> type) {
            this(type, null);
        }

        StubCodec(Class<T> type, Parameter parameter) {
            super(type);
            this.parameter = parameter;
        }

        @Override
        Parameter doEncode(T value) {
            this.value = value;
            return this.parameter;
        }

    }

}
