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
package org.jdbi.v3.jackson2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.meta.Beta;

/**
 * Configuration class for Jackson 2 integration.
 */
@Beta
public class Jackson2Config implements JdbiConfig<Jackson2Config> {
    private ObjectMapper mapper;
    private Class<?> serializationView;
    private Class<?> deserializationView;

    public Jackson2Config() {
        this.mapper = new ObjectMapper();
    }

    private Jackson2Config(Jackson2Config other) {
        this.mapper = other.mapper;
        this.serializationView = other.serializationView;
        this.deserializationView = other.deserializationView;
    }

    /**
     * Set the {@link ObjectMapper} to use for json conversion.
     * @param mapper the mapper to use
     * @return this
     */
    public Jackson2Config setMapper(ObjectMapper mapper) {
        this.mapper = mapper;
        return this;
    }

    /**
     * @return the object mapper to use for json conversion
     */
    public ObjectMapper getMapper() {
        return mapper;
    }

    /**
     * Set both serialization and deserialization {@code @JsonView} to the given class.
     * @param view the view class
     * @return this
     */
    public Jackson2Config setView(Class<?> view) {
        return setSerializationView(view).setDeserializationView(view);
    }

    /**
     * Set the {@code @JsonView} used to serialize.
     * @param serializationView the serialization view
     * @return this
     */
    public Jackson2Config setSerializationView(Class<?> serializationView) {
        this.serializationView = serializationView;
        return this;
    }

    /**
     * @return the current {@code @JsonView} used for serialization
     */
    public Class<?> getSerializationView() {
        return serializationView;
    }

    /**
     * Set the {@code @JsonView} used to deserialize.
     * @param deserializationView the serialization view
     * @return this
     */
    public Jackson2Config setDeserializationView(Class<?> deserializationView) {
        this.deserializationView = deserializationView;
        return this;
    }

    /**
     * @return the current {@code @JsonView} used for deserialization
     */
    public Class<?> getDeserializationView() {
        return deserializationView;
    }

    @Override
    public Jackson2Config createCopy() {
        return new Jackson2Config(this);
    }
}
