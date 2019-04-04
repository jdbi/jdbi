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

import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.result.UnableToProduceResultException;
import org.jdbi.v3.json.JsonMapper;

class JacksonJsonMapper implements JsonMapper {
    @Override
    public String toJson(Type type, Object value, ConfigRegistry config) {
        try {
            Jackson2Config cfg = config.get(Jackson2Config.class);
            ObjectWriter writer = cfg.getMapper().writerFor(cfg.getMapper().constructType(type));
            Class<?> view = cfg.getSerializationView();
            if (view != null) {
                writer = writer.withView(view);
            }
            return writer.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new UnableToProduceResultException(e);
        }
    }

    @Override
    public Object fromJson(Type type, String json, ConfigRegistry config) {
        try {
            Jackson2Config cfg = config.get(Jackson2Config.class);
            ObjectReader reader = cfg.getMapper().readerFor(cfg.getMapper().constructType(type));
            Class<?> view = cfg.getDeserializationView();
            if (view != null) {
                reader = reader.withView(view);
            }
            return reader.readValue(json);
        } catch (IOException e) {
            throw new UnableToProduceResultException(e);
        }
    }
}
