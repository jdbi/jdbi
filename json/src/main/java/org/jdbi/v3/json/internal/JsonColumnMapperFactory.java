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
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.internal.JdbiOptionals;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.result.UnableToProduceResultException;
import org.jdbi.v3.json.Json;
import org.jdbi.v3.json.JsonConfig;

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
        if (String.class.equals(type)) {
            return Optional.empty();
        }

        return Optional.of((rs, i, ctx) -> {
            // look for specialized json support first, revert to simple String mapping if absent
            ColumnMapper<String> jsonStringMapper = JdbiOptionals.findFirstPresent(
                () -> ctx.findColumnMapperFor(QualifiedType.of(String.class).with(Json.class)).map(JsonColumnMapperFactory::cast),
                () -> ctx.findColumnMapperFor(String.class))
                    .orElseThrow(() -> new UnableToProduceResultException(JSON_NOT_RETRIEVABLE, ctx));

            String json = jsonStringMapper.map(rs, i, ctx);

            return json == null ? null : ctx.getConfig(JsonConfig.class).getJsonMapper().fromJson(type, json, ctx);
        });
    }

    // TODO improve generic type support on qualtype and Type methods to remove this
    @SuppressWarnings("unchecked")
    private static ColumnMapper<String> cast(ColumnMapper<?> mapper) {
        return (ColumnMapper<String>) mapper;
    }
}
