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
import org.jdbi.v3.core.internal.JdbiOptionals;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.json.Json;
import org.jdbi.v3.json.JsonConfig;

/**
 * converts a value object to json text and delegates to another factory to perform the {@code (@Json) String} binding
 */
@Json
public class JsonArgumentFactory implements ArgumentFactory {
    private static final String JSON_NOT_STORABLE = String.format(
        "No argument factory found for `@%s String` or 'String'",
        Json.class.getSimpleName()
    );

    @Override
    public Optional<Argument> build(Type type, Object value, ConfigRegistry config) {
        if (String.class.equals(type)) {
            return Optional.empty();
        }

        return Optional.of((pos, stmt, ctx) -> {
            String json = value == null ? null : ctx.getConfig(JsonConfig.class).getJsonMapper().toJson(type, value, ctx);

            // look for specialized json support first, revert to simple String binding if absent
            Argument stringBinder = JdbiOptionals.findFirstPresent(
                () -> ctx.findArgumentFor(QualifiedType.of(String.class).with(Json.class), json),
                () -> ctx.findArgumentFor(String.class, json))
                    .orElseThrow(() -> new UnableToCreateStatementException(JSON_NOT_STORABLE));

            stringBinder.apply(pos, stmt, ctx);
        });
    }
}
