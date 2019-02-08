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

import java.lang.reflect.Type;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.json.JsonMapper;

class GsonJsonMapper implements JsonMapper {
    @Override
    public String toJson(Type type, Object value, ConfigRegistry config) {
        return config.get(Gson2Config.class).getGson().toJson(value, type);
    }

    @Override
    public Object fromJson(Type type, String json, ConfigRegistry config) {
        return config.get(Gson2Config.class).getGson().fromJson(json, type);
    }
}
