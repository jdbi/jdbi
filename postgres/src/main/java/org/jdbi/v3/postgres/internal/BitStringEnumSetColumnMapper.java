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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

import static java.util.stream.Collectors.toCollection;

public class BitStringEnumSetColumnMapper<E extends Enum<E>> implements ColumnMapper<EnumSet<E>> {
    private final Class<E> enumType;
    private final E[] enumConstants;

    BitStringEnumSetColumnMapper(Class<E> enumType) {
        this.enumType = enumType;
        enumConstants = enumType.getEnumConstants();
    }

    @Override
    public EnumSet<E> map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
        String bits = r.getString(columnNumber);

        if (bits == null) {
            return null;
        }
        if (bits.length() != enumConstants.length) {
            throw new IllegalArgumentException("bit string \"" + bits + "\" for " + enumType + " should not contain " + bits.length() + " characters");
        }

        return IntStream.range(0, bits.length())
            .peek(checkIfBitIn(bits))
            .filter(i -> bits.charAt(i) == '1')
            .mapToObj(i -> enumConstants[i])
            .collect(toCollection(() -> EnumSet.noneOf(enumType)));
    }

    private static IntConsumer checkIfBitIn(String bits) {
        return i -> {
            char bit = bits.charAt(i);
            if (bit != '0' && bit != '1') {
                throw new IllegalArgumentException("bit string \"" + bits + "\" contains non-bit character " + bit);
            }
        };
    }
}
