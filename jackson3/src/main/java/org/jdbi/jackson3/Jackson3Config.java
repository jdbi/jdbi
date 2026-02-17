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

import org.jdbi.core.config.JdbiConfig;
import tools.jackson.databind.ObjectMapper;

/**
 * Configuration class for Jackson 3 integration.
 */
public class Jackson3Config implements JdbiConfig<Jackson3Config> {
    private ObjectMapper mapper;
    private Class<?> serializationView;
    private Class<?> deserializationView;
    private boolean useStaticType = true;

    public Jackson3Config() {
        this.mapper = new ObjectMapper();
    }

    private Jackson3Config(final Jackson3Config other) {
        this.mapper = other.mapper;
        this.serializationView = other.serializationView;
        this.deserializationView = other.deserializationView;
        this.useStaticType = other.useStaticType;
    }

    /**
     * Set the {@link ObjectMapper} to use for json conversion.
     * @param mapper the mapper to use
     * @return this
     */
    public Jackson3Config setMapper(final ObjectMapper mapper) {
        this.mapper = mapper;
        return this;
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
     * Set both serialization and deserialization {@code @JsonView} to the given class.
     * @param view the view class
     * @return this
     */
    public Jackson3Config setView(final Class<?> view) {
        return setSerializationView(view).setDeserializationView(view);
    }

    /**
     * Set the {@code @JsonView} used to serialize.
     * @param serializationView the serialization view
     * @return this
     */
    public Jackson3Config setSerializationView(final Class<?> serializationView) {
        this.serializationView = serializationView;
        return this;
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
     * Set the {@code @JsonView} used to deserialize.
     * @param deserializationView the serialization view
     * @return this
     */
    public Jackson3Config setDeserializationView(final Class<?> deserializationView) {
        this.deserializationView = deserializationView;
        return this;
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
     * Use static type provided for serialization. Better performance and supports generic container types,
     * but inhibits discovery of custom polymorphic types.
     * @param useStaticType whether to prefer using static type information
     * @return this
     */
    public Jackson3Config setUseStaticType(final boolean useStaticType) {
        this.useStaticType = useStaticType;
        return this;
    }

    /**
     * @return whether Jackson prefers to use the static type instaed of dynamic type
     */
    public boolean isUseStaticType() {
        return useStaticType;
    }

    @Override
    public Jackson3Config createCopy() {
        return new Jackson3Config(this);
    }
}
