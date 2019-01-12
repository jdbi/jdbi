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

    public Jackson2Config() {
        this.mapper = new ObjectMapper();
    }

    private Jackson2Config(Jackson2Config other) {
        this.mapper = other.mapper;
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

    @Override
    public Jackson2Config createCopy() {
        return new Jackson2Config(this);
    }
}
