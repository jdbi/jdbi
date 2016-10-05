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
import org.jdbi.v3.core.argument.Argument;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.EnumSet;

public class EnumSetArgument implements Argument {

    private Enum<?>[] enumConstants;
    private EnumSet<?> elements;

    public EnumSetArgument(Class<Enum<?>> enumType, EnumSet<?> elements) {
        this.elements = elements;
        enumConstants = enumType.getEnumConstants();
    }

    @Override
    public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException {
        if (elements == null) {
            statement.setString(position, null);
            return;
        }

        char[] bits = new char[enumConstants.length];
        for (int i = 0; i < enumConstants.length; i++) {
            bits[i] = elements.contains(enumConstants[i]) ? '1' : '0';
        }
        statement.setString(position, new String(bits));
    }
}
