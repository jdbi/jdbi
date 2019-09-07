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
package org.jdbi.v3.postgres.internal;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.EnumSet;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.statement.StatementContext;

import static java.util.stream.Collectors.joining;

public class BitStringEnumSetArgument<E extends Enum<E>> implements Argument {
    private final E[] enumConstants;
    private final EnumSet<E> elements;

    BitStringEnumSetArgument(Class<E> enumType, EnumSet<E> elements) {
        enumConstants = enumType.getEnumConstants();
        this.elements = elements;
    }

    @Override
    public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException {
        String bits = elements == null ? null : Arrays.stream(enumConstants)
            .map(value -> elements.contains(value) ? "1" : "0")
            .collect(joining());

        statement.setString(position, bits);
    }
}
