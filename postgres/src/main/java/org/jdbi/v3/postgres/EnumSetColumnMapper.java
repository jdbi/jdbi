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
package org.jdbi.v3.postgres;

import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.mapper.ColumnMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;

public class EnumSetColumnMapper<E extends Enum<E>> implements ColumnMapper<EnumSet<E>> {

    private Class<E> enumType;
    private E[] enumConstants;

    public EnumSetColumnMapper(Class<E> enumType) {
        this.enumType = enumType;
        enumConstants = enumType.getEnumConstants();
    }

    @Override
    public EnumSet<E> map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
        String bits = r.getString(columnNumber);
        if (bits == null) {
            return null;
        }

        EnumSet<E> elements = EnumSet.noneOf(enumType);
        for (int i = 0; i < bits.length(); i++) {
            char bit = bits.charAt(i);
            if (bit != '0' && bit != '1') {
                throw new IllegalArgumentException("Wrong element in a bit string: " + bits);
            }
            if (bit == '1') {
                elements.add(enumConstants[i]);
            }
        }
        return elements;
    }
}
