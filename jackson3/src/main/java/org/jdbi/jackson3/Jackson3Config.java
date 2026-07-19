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
package org.jdbi.jackson3;

import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import org.jdbi.core.config.JdbiConfig;
import tools.jackson.databind.ObjectMapper;

/**
 * Configuration class for Jackson 3 integration.
 */
public final class Jackson3Config implements JdbiConfig<Jackson3Config> {
    private final ObjectMapper mapper;
    private final Class<?> serializationView;
    private final Class<?> deserializationView;
    private final boolean useStaticType;

    public Jackson3Config() {
        this(new ObjectMapper(), null, null, true);
    }

    private Jackson3Config(final ObjectMapper mapper, final Class<?> serializationView, final Class<?> deserializationView, final boolean useStaticType) {
        this.mapper = mapper;
        this.serializationView = serializationView;
        this.deserializationView = deserializationView;
        this.useStaticType = useStaticType;
    }

    /**
     * Returns a copy of this configuration using the given {@link ObjectMapper} for json conversion.
     * @param mapper the mapper to use
     * @return the derived configuration
     */
    @CheckReturnValue
    public Jackson3Config mapper(final ObjectMapper mapper) {
        return new Jackson3Config(mapper, serializationView, deserializationView, useStaticType);
    }

    /**
     * Returns the object mapper to use for json conversion.
     *
     * @return the object mapper to use for json conversion.
     */
    public ObjectMapper getMapper() {
        return mapper;
    }

    /**
     * Returns a copy of this configuration setting both serialization and deserialization {@code @JsonView} to
     * the given class.
     * @param view the view class
     * @return the derived configuration
     */
    @CheckReturnValue
    public Jackson3Config view(final Class<?> view) {
        return serializationView(view).deserializationView(view);
    }

    /**
     * Returns a copy of this configuration using the given {@code @JsonView} to serialize.
     * @param serializationView the serialization view
     * @return the derived configuration
     */
    @CheckReturnValue
    public Jackson3Config serializationView(final Class<?> serializationView) {
        return new Jackson3Config(mapper, serializationView, deserializationView, useStaticType);
    }

    /**
     * Returns the current {@code @JsonView} used for serialization.
     *
     * @return The current {@code @JsonView} used for serialization.
     */
    public Class<?> getSerializationView() {
        return serializationView;
    }

    /**
     * Returns a copy of this configuration using the given {@code @JsonView} to deserialize.
     * @param deserializationView the serialization view
     * @return the derived configuration
     */
    @CheckReturnValue
    public Jackson3Config deserializationView(final Class<?> deserializationView) {
        return new Jackson3Config(mapper, serializationView, deserializationView, useStaticType);
    }

    /**
     * Returns the current {@code @JsonView} used for deserialization.
     *
     * @return the current {@code @JsonView} used for deserialization.
     */
    public Class<?> getDeserializationView() {
        return deserializationView;
    }

    /**
     * Returns a copy of this configuration controlling whether the static type is used for serialization. Using
     * the static type gives better performance and supports generic container types, but inhibits discovery of
     * custom polymorphic types.
     * @param useStaticType whether to prefer using static type information
     * @return the derived configuration
     */
    @CheckReturnValue
    public Jackson3Config useStaticType(final boolean useStaticType) {
        return new Jackson3Config(mapper, serializationView, deserializationView, useStaticType);
    }

    /**
     * @return whether Jackson prefers to use the static type instaed of dynamic type
     */
    public boolean isUseStaticType() {
        return useStaticType;
    }

    @Override
    public Jackson3Config createCopy() {
        // Immutable: safe to share across registries.
        return this;
    }
}
