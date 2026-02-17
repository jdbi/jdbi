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
package org.jdbi.jackson2;

import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.result.UnableToProduceResultException;
import org.jdbi.json.JsonMapper;

class JacksonJsonMapper implements JsonMapper {
    @Override
    public TypedJsonMapper forType(Type type, ConfigRegistry config) {
        return new TypedJsonMapper() {
            private final Jackson2Config jacksonConfig = config.get(Jackson2Config.class);
            private final ObjectMapper mapper = jacksonConfig.getMapper();
            private final JavaType mappedType = mapper.constructType(type);
            private final ObjectReader reader = mapper.readerFor(mappedType);
            private final ObjectWriter writer =
                    jacksonConfig.isUseStaticType()
                            ? mapper.writerFor(mappedType)
                            : mapper.writer();

            @Override
            public String toJson(Object value, ConfigRegistry config) {
                final Class<?> view = config.get(Jackson2Config.class).getSerializationView();
                final ObjectWriter viewWriter =
                          view == null
                        ? writer
                        : writer.withView(view);
                try {
                    return viewWriter.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    throw new UnableToProduceResultException(e);
                }
            }

            @Override
            public Object fromJson(String json, ConfigRegistry config) {
                final Class<?> view = config.get(Jackson2Config.class).getDeserializationView();
                final ObjectReader viewReader =
                          view == null
                        ? reader
                        : reader.withView(view);
                try {
                    return viewReader.readValue(json);
                } catch (IOException e) {
                    throw new UnableToProduceResultException(e);
                }
            }
        };
    }
}
