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
package org.jdbi.v3.core.codec;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.QualifiedArgumentFactory;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.QualifiedColumnMapperFactory;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.meta.Beta;

/**
 * CodecFactory provides column mappers and arguments for bidirectional mapping types to database columns.
 * <p>
 * This class is immutable and thread safe.
 */
@ThreadSafe
@Beta
public class CodecFactory implements QualifiedColumnMapperFactory, QualifiedArgumentFactory.Preparable {

    private final ConcurrentMap<QualifiedType<?>, Codec<?>> codecMap = new ConcurrentHashMap<>();

    /**
     * Returns a builder for fluent API.
     *
     * @return Builder for fluent API.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static CodecFactory forSingleCodec(QualifiedType<?> type, Codec<?> codec) {
        return new CodecFactory(Collections.singletonMap(type, codec));
    }

    /**
     * Create a new CodecFactory.
     *
     * @param codecMap
     */
    public CodecFactory(final Map<QualifiedType<?>, Codec<?>> codecMap) {
        requireNonNull(codecMap, "codecMap is null");
        this.codecMap.putAll(codecMap);
    }

    @Override
    public Optional<Function<Object, Argument>> prepare(final QualifiedType<?> type, final ConfigRegistry config) {
        return Optional.of(type).map(codecMap::get).map(key -> (Function<Object, Argument>) key.getArgumentFunction());
    }

    @Override
    public Collection<QualifiedType<?>> prePreparedTypes() {
        return codecMap.keySet();
    }

    @Override
    public Optional<Argument> build(final QualifiedType<?> type, final Object value, final ConfigRegistry config) {
        return prepare(type, config).map(f -> f.apply(value));
    }

    @Override
    public Optional<ColumnMapper<?>> build(final QualifiedType<?> type, final ConfigRegistry config) {
        return Optional.of(type).map(codecMap::get).map(Codec::getColumnMapper);
    }

    /**
     * Fluent Builder for {@link CodecFactory}.
     */
    @NotThreadSafe
    public static final class Builder {

        private final Map<QualifiedType<?>, Codec<?>> codecMap = new HashMap<>();

        Builder() {
        }

        /**
         * Add a codec for a {@link QualifiedType}.
         */
        public Builder addCodec(final QualifiedType<?> type, final Codec<?> codec) {
            requireNonNull(type, "type is null");
            requireNonNull(codec, "codec is null");
            codecMap.put(type, codec);

            return this;
        }

        /**
         * Add a codec for a {@link Type}.
         */
        public Builder addCodec(final Type type, final Codec<?> codec) {
            requireNonNull(type, "type is null");
            requireNonNull(codec, "codec is null");
            codecMap.put(QualifiedType.of(type), codec);

            return this;
        }

        /**
         * Add a codec for a {@link GenericType}.
         */
        public Builder addCodec(final GenericType<?> type, final Codec<?> codec) {
            requireNonNull(type, "type is null");
            requireNonNull(codec, "codec is null");
            codecMap.put(QualifiedType.of(type.getType()), codec);

            return this;
        }

        public CodecFactory build() {
            return new CodecFactory(codecMap);
        }
    }

}
