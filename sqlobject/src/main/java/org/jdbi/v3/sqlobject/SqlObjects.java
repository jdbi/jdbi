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

import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.sqlobject.locator.AnnotationSqlLocator;
import org.jdbi.v3.sqlobject.locator.SqlLocator;

/**
 * Configuration class for SQL objects
 */
public class SqlObjects implements JdbiConfig<SqlObjects> {
    private SqlLocator sqlLocator;
    private DefaultHandlerFactory defaultHandlerFactory;

    public SqlObjects() {
        sqlLocator = new AnnotationSqlLocator();
        defaultHandlerFactory = new DefaultMethodHandlerFactory();
    }

    private SqlObjects(SqlObjects that) {
        sqlLocator = that.sqlLocator;
        defaultHandlerFactory = that.defaultHandlerFactory;
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

    /**
     * Returns the configured {@link DefaultHandlerFactory}. The default is {@link DefaultMethodHandlerFactory}.
     *
     * @return the configured {@link DefaultHandlerFactory}.
     */
    public DefaultHandlerFactory getDefaultHandlerFactory() {
        return defaultHandlerFactory;
    }

    /**
     * Configures SqlObject to use the given {@link DefaultHandlerFactory}.
     * This setting is locked in for an sql object type the first time it is instantiated
     * (or invoked, for on-demand). This setting must be set on {@link org.jdbi.v3.core.Jdbi} level,
     * before the first sql object is created.
     *
     * @param defaultHandlerFactory the new default handler factory.
     * @return this {@link SqlObjects}.
     */
    public SqlObjects setDefaultHandlerFactory(DefaultHandlerFactory defaultHandlerFactory) {
        this.defaultHandlerFactory = Objects.requireNonNull(defaultHandlerFactory);
        return this;
    }

    @Override
    public SqlObjects createCopy() {
        return new SqlObjects(this);
    }
}
