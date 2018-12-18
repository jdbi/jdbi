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
import org.jdbi.v3.json.Json;
import org.jdbi.v3.json.JsonConfig;

@Json
public class JsonColumnMapperFactory implements ColumnMapperFactory {
    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        return Optional.of((r, i, c) -> {
            final ColumnMapper<String> jsonStringMapper = JdbiOptionals.findFirstPresent(
                () -> c.findColumnMapperFor(QualifiedType.of(Json.class)).map(JsonColumnMapperFactory::bludgeon),
                () -> c.findColumnMapperFor(String.class))
                    .orElseThrow(() -> new IllegalStateException("No column mapper found for '@Json String' or 'String', this really shouldn't happen..."));
            final String jsonValue = jsonStringMapper.map(r, i, c);
            return jsonValue == null ? null : c.getConfig(JsonConfig.class).getJsonMapper().fromJson(type, jsonValue, c);
        });
    }

    @SuppressWarnings("unchecked")
    private static ColumnMapper<String> bludgeon(ColumnMapper<?> mapper) {
        return (ColumnMapper<String>) mapper;
    }
}
