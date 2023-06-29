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
import java.util.function.Function;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.internal.JdbiOptionals;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.json.EncodedJson;
import org.jdbi.v3.json.Json;
import org.jdbi.v3.json.JsonConfig;
import org.jdbi.v3.json.JsonMapper.TypedJsonMapper;

/**
 * converts a value object to json text and delegates to another factory to perform the {@code (@Json) String} binding
 */
@Json
public class JsonArgumentFactory implements ArgumentFactory.Preparable {
    public static final QualifiedType<String> ENCODED_JSON = QualifiedType.of(String.class).with(EncodedJson.class);

    private static final String JSON_NOT_STORABLE = String.format(
        "No argument factory found for `@%s String` or 'String'",
        EncodedJson.class.getSimpleName()
    );

    @Override
    public Optional<Function<Object, Argument>> prepare(Type type, ConfigRegistry config) {
        TypedJsonMapper mapper = config.get(JsonConfig.class).getJsonMapper().forType(type, config);
        Arguments a = config.get(Arguments.class);
        // look for specialized json support first, revert to simple String binding if absent
        Function<Object, Argument> bindJson = JdbiOptionals.findFirstPresent(
                () -> a.prepareFor(ENCODED_JSON),
                () -> a.prepareFor(String.class))
            .orElseThrow(() -> new UnableToCreateStatementException(JSON_NOT_STORABLE));
        return Optional.of((Function<Object, Argument>) value -> {
            String nullableJson = value == null ? null : mapper.toJson(value, config);
            String json = "null".equals(nullableJson) ? null : nullableJson; // json null -> sql null
            return bindJson.apply(json);
        });
    }
}
