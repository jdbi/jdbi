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
package org.jdbi.v3.core.qualifier;

import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;

@Reversed
class ReversedStringMapperFactory implements ColumnMapperFactory {
    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        if (String.class.equals(type)) {
            return Optional.of((rs, col, ctx) -> Reverser.reverse(rs.getString(col)));
        }
        return Optional.empty();
    }
}
