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
package org.jdbi.sqlobject.customizer.internal;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import org.jdbi.core.argument.Arguments;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.mapper.reflect.internal.PojoProperties;
import org.jdbi.core.mapper.reflect.internal.PojoProperties.PojoProperty;
import org.jdbi.core.mapper.reflect.internal.PojoTypes;
import org.jdbi.core.statement.SqlStatement;
import org.jdbi.sqlobject.customizer.SqlStatementParameterCustomizer;

public final class PojoWarmingCustomizer {
    private PojoWarmingCustomizer() {}

    public static SqlStatementParameterCustomizer of(Type pojoType, SqlStatementParameterCustomizer customizer) {
        return new SqlStatementParameterCustomizer() {
            @Override
            public void apply(SqlStatement<?> stmt, Object arg) throws SQLException {
                customizer.apply(stmt, arg);
            }

            @Override
            public void warm(ConfigRegistry config) {
                Arguments arguments = config.get(Arguments.class);
                config.get(PojoTypes.class)
                        .findFor(pojoType)
                        .map(Stream::of)
                        .orElseGet(Stream::empty)
                        .map(PojoProperties::getProperties)
                        .map(Map::values)
                        .flatMap(Collection::stream)
                        .map(PojoProperty::getQualifiedType)
                        .forEach(arguments::prepareFor);
            }
        };
    }
}
