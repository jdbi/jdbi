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
package org.jdbi.v3.gson2;

import java.io.IOException;
import java.lang.reflect.Type;

import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.result.UnableToProduceResultException;
import org.jdbi.v3.json.JsonMapper;

class GsonJsonMapper implements JsonMapper {
    @Override
    public TypedJsonMapper forType(Type type, ConfigRegistry config) {
        return new TypedJsonMapper() {
            @SuppressWarnings("rawtypes")
            private final TypeAdapter adapter = config.get(Gson2Config.class)
                    .getGson().getAdapter(TypeToken.get(type));

            @SuppressWarnings("unchecked")
            @Override
            public String toJson(Object value, ConfigRegistry config) {
                return adapter.toJson(value);
            }

            @Override
            public Object fromJson(String json, ConfigRegistry config) {
                try {
                    return adapter.fromJson(json);
                } catch (IOException e) {
                    throw new UnableToProduceResultException(e);
                }
            }
        };
    }
}
