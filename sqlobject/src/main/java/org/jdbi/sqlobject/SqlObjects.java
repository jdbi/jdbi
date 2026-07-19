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
package org.jdbi.sqlobject;

import java.util.Objects;

import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import org.jdbi.core.config.JdbiConfig;
import org.jdbi.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.sqlobject.locator.AnnotationSqlLocator;
import org.jdbi.sqlobject.locator.SqlLocator;
import org.jdbi.sqlobject.statement.BindParameterCustomizerFactory;
import org.jdbi.sqlobject.statement.ParameterCustomizerFactory;

/**
 * Configuration class for SQL objects.
 */
public final class SqlObjects implements JdbiConfig<SqlObjects> {
    private final SqlLocator sqlLocator;
    private final ParameterCustomizerFactory defaultParameterCustomizerFactory;

    public SqlObjects() {
        this(new AnnotationSqlLocator(), new BindParameterCustomizerFactory());
    }

    private SqlObjects(SqlLocator sqlLocator, ParameterCustomizerFactory defaultParameterCustomizerFactory) {
        this.sqlLocator = sqlLocator;
        this.defaultParameterCustomizerFactory = defaultParameterCustomizerFactory;
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
     * Returns a copy of this configuration using the given {@link SqlLocator}.
     *
     * @param sqlLocator the new SQL locator.
     * @return the derived configuration
     */
    @CheckReturnValue
    public SqlObjects sqlLocator(SqlLocator sqlLocator) {
        return new SqlObjects(Objects.requireNonNull(sqlLocator), defaultParameterCustomizerFactory);
    }

    /**
     * Returns the configured {@link ParameterCustomizerFactory} used to bind sql statement parameters
     * when parameter is not explicitly annotated. By default it is configured as an instance of {@link BindParameterCustomizerFactory}.
     *
     * @return the configured {@link SqlStatementCustomizerFactory}.
     */
    public ParameterCustomizerFactory getDefaultParameterCustomizerFactory() {
        return defaultParameterCustomizerFactory;
    }

    /**
     * Returns a copy of this configuration using the given default parameter customizer factory.
     *
     * @param defaultParameterCustomizerFactory the new default parameter customizer factory.
     * @return the derived configuration
     */
    @CheckReturnValue
    public SqlObjects defaultParameterCustomizerFactory(ParameterCustomizerFactory defaultParameterCustomizerFactory) {
        return new SqlObjects(sqlLocator, defaultParameterCustomizerFactory);
    }

    @Override
    public SqlObjects createCopy() {
        // Immutable: safe to share across registries.
        return this;
    }
}
