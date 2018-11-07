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
package org.jdbi.v3.sqlobject.customizer;

import java.sql.SQLException;

import org.jdbi.v3.core.statement.SqlStatement;

/**
 * Customize a {@link SqlStatement} according to the value of an annotated parameter.
 */
public interface SqlStatementParameterCustomizer<T> {
    /**
     * Applies the customization to the SQL statement using the argument passed to the method.
     * @param stmt the statement being customized
     * @param arg the argument passed to the method
     * @throws SQLException will abort statement creation
     */
    void apply(SqlStatement<?> stmt, T arg) throws SQLException;
}
