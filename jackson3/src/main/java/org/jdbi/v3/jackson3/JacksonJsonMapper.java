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
package org.jdbi.v3.jackson3;

import java.lang.reflect.Type;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.result.UnableToProduceResultException;
import org.jdbi.v3.json.JsonMapper;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ObjectWriter;

class JacksonJsonMapper implements JsonMapper {
    @Override
    public TypedJsonMapper forType(final Type type, final ConfigRegistry config) {
        return new TypedJsonMapper() {
            private final ObjectMapper mapper = config.get(Jackson3Config.class).getMapper();
            private final JavaType mappedType = mapper.constructType(type);
            private final ObjectReader reader = mapper.readerFor(mappedType);
            private final ObjectWriter writer = mapper.writerFor(mappedType);

            @Override
            public String toJson(final Object value, final ConfigRegistry config) {
                final Class<?> view = config.get(Jackson3Config.class).getSerializationView();
                final ObjectWriter viewWriter =
                          view == null
                        ? writer
                        : writer.withView(view);
                try {
                    return viewWriter.writeValueAsString(value);
                } catch (final JacksonException e) {
                    throw new UnableToProduceResultException(e);
                }
            }

            @Override
            public Object fromJson(final String json, final ConfigRegistry config) {
                final Class<?> view = config.get(Jackson3Config.class).getDeserializationView();
                final ObjectReader viewReader =
                          view == null
                        ? reader
                        : reader.withView(view);
                try {
                    return viewReader.readValue(json);
                } catch (final JacksonException e) {
                    throw new UnableToProduceResultException(e);
                }
            }
        };
    }
}
