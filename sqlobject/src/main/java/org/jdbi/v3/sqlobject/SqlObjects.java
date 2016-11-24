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
package org.jdbi.v3.sqlobject;

import java.util.Objects;

import org.jdbi.v3.core.JdbiConfig;
import org.jdbi.v3.sqlobject.locator.AnnotationSqlLocator;
import org.jdbi.v3.sqlobject.locator.SqlLocator;

/**
 * Configuration class for SQL objects
 */
public class SqlObjects implements JdbiConfig<SqlObjects> {
    private SqlLocator sqlLocator;

    public SqlObjects() {
        sqlLocator = new AnnotationSqlLocator();
    }

    private SqlObjects(SqlObjects parent) {
        this.sqlLocator = parent.sqlLocator;
    }

    /**
     * Returns the configured {@link SqlLocator}. The default SQL locator is {@link AnnotationSqlLocator}.
     *
     * @return the configured {@link SqlLocator}.
     */
    public SqlLocator getSqlLocator() {
        return sqlLocator;
    }

    /**
     * Configures SqlObject to use the given {@link SqlLocator}.
     *
     * @param sqlLocator the new SQL locator.
     * @return this {@link SqlObjects}.
     */
    public SqlObjects setSqlLocator(SqlLocator sqlLocator) {
        this.sqlLocator = Objects.requireNonNull(sqlLocator);
        return this;
    }

    @Override
    public SqlObjects createChild() {
        return new SqlObjects(this);
    }
}
