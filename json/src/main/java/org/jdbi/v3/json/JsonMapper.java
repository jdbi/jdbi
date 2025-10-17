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
package org.jdbi.v3.json;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import org.jdbi.v3.core.config.ConfigRegistry;

/**
 * Deserializes JSON to Java objects, and serializes Java objects to JSON.
 *
 * Implement this interface and {@link JsonConfig#setJsonMapper(JsonMapper)} it
 * to be able to convert objects to/from JSON between your application and database.
 *
 * jdbi3-jackson2 and jdbi3-gson2 are readily available for this.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface JsonMapper {
    @Deprecated(since = "3.40.0", forRemoval = true)
    default String toJson(Type type, Object value, ConfigRegistry config) {
        return forType(type, Collections.emptySet(), config).toJson(value, config);
    }

    @Deprecated(since = "3.40.0", forRemoval = true)
    default Object fromJson(Type type, String json, ConfigRegistry config) {
        return forType(type, Collections.emptySet(), config).fromJson(json, config);
    }

    TypedJsonMapper forType(Type type, Set<? extends Annotation> annotations, ConfigRegistry config);

    interface TypedJsonMapper {
        String toJson(Object value, ConfigRegistry config);
        Object fromJson(String json, ConfigRegistry config);
    }
}
