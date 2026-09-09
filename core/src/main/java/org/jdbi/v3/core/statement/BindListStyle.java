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
package org.jdbi.v3.core.statement;

import org.jdbi.v3.meta.Alpha;

/**
 * Describes how {@link SqlStatement#bindList} renders the bound parameter names into the defined attribute.
 * The style must match the SQL around the attribute, so it is normally set per statement with
 * {@code configure(SqlStatements.class, c -> c.setBindListStyle(...))} or {@code @BindList(style = ...)}.
 *
 * @see SqlStatements#setBindListStyle(BindListStyle)
 */
@Alpha
public enum BindListStyle {
    /**
     * <p>Render each element as a bare parameter.</p>
     * <p>
     * {@code select * from things where x in (<ids>)} renders as {@code select * from things where x in (:__ids_0,:__ids_1)}
     */
    PLAIN("", ""),
    /**
     * <p>Render each element as a single-column row, for use with the SQL {@code VALUES} list constructor.</p>
     * <p>
     * {@code select * from (values <ids>) as t(id)} renders as {@code select * from (values (:__ids_0),(:__ids_1)) as t(id)}
     * <p>
     * {@link EmptyHandling#NULL_KEYWORD} renders {@code values null}, which is not valid SQL. Handle an empty list with
     * {@link EmptyHandling#THROW} or {@link EmptyHandling#DEFINE_NULL} and a template conditional instead.
     */
    ROWS("(", ")");

    private final String elementPrefix;
    private final String elementSuffix;

    BindListStyle(String elementPrefix, String elementSuffix) {
        this.elementPrefix = elementPrefix;
        this.elementSuffix = elementSuffix;
    }

    void appendElement(StringBuilder target, String parameterName) {
        target.append(elementPrefix).append(parameterName).append(elementSuffix);
    }
}
