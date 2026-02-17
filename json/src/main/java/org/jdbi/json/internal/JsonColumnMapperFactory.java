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
package org.jdbi.json.internal;

import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.internal.JdbiOptionals;
import org.jdbi.core.mapper.ColumnMapper;
import org.jdbi.core.mapper.ColumnMapperFactory;
import org.jdbi.core.mapper.ColumnMappers;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.core.result.UnableToProduceResultException;
import org.jdbi.json.EncodedJson;
import org.jdbi.json.Json;
import org.jdbi.json.JsonConfig;
import org.jdbi.json.JsonMapper.TypedJsonMapper;

/**
 * converts a {@code (@Json) String} fetched by another mapper into a value object
 */
@Json
public class JsonColumnMapperFactory implements ColumnMapperFactory {
    private static final String JSON_NOT_RETRIEVABLE = String.format(
        "No column mapper found for '@%s String', or 'String'",
        Json.class.getSimpleName()
    );

    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        ColumnMappers cm = config.get(ColumnMappers.class);
        // look for specialized json support first, revert to simple String mapping if absent
        ColumnMapper<String> jsonStringMapper = JdbiOptionals.findFirstPresent(
                () -> cm.findFor(QualifiedType.of(String.class).with(EncodedJson.class)),
                () -> cm.findFor(String.class))
                .orElseThrow(() -> new UnableToProduceResultException(JSON_NOT_RETRIEVABLE));

        final TypedJsonMapper mapper = config.get(JsonConfig.class).getJsonMapper().forType(type, config);
        return Optional.of((rs, i, ctx) ->
            mapper.fromJson(
                    Optional.ofNullable(jsonStringMapper.map(rs, i, ctx))
                            .orElse("null"), // sql null -> json null
                    config));
    }
}
