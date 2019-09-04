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
package org.jdbi.v3.core.mapper;

import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.qualifier.NVarchar;
import org.jdbi.v3.core.qualifier.QualifiedType;

/**
 * Column mapper for Java {@code @NVarchar String} qualified type.
 */
class NVarcharMapper implements QualifiedColumnMapperFactory {
    private static final QualifiedType<String> NSTRING =
            QualifiedType.of(String.class).with(NVarchar.class);

    @Override
    public Optional<ColumnMapper<?>> build(QualifiedType<?> type, ConfigRegistry config) {
        return NSTRING.equals(type)
                ? Optional.of((rs, c, ctx) -> rs.getNString(c))
                : Optional.empty();
    }
}
