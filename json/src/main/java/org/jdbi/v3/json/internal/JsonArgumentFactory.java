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
package org.jdbi.v3.json.internal;

import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.json.Json;
import org.jdbi.v3.json.JsonConfig;

@Json
class JsonArgumentFactory implements ArgumentFactory {
    @Override
    public Optional<Argument> build(Type type, Object value, ConfigRegistry config) {
        return Optional.of((p, s, c) -> {
            c.findArgumentFor(
                    Json.class,
                    value == null ? null : c.getConfig(JsonConfig.class).getJsonMapper().toJson(type, value, c))
                .orElseThrow(() -> new IllegalStateException("No Json binder registered, did you install e.g. PostgresPlugin?"))
                .apply(p, s, c);
        });
    }
}
