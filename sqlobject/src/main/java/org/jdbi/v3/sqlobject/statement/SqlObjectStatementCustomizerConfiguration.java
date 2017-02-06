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
package org.jdbi.v3.sqlobject.statement;

import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.sqlobject.SqlObjects;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;

public class SqlObjectStatementCustomizerConfiguration implements JdbiConfig<SqlObjectStatementCustomizerConfiguration> {
    private SqlStatementCustomizerFactory defaultParameterCustomizerFactory;

    public SqlObjectStatementCustomizerConfiguration() {
        defaultParameterCustomizerFactory = new Bind.Factory();
    }

    private SqlObjectStatementCustomizerConfiguration(SqlObjectStatementCustomizerConfiguration other) {
        this.defaultParameterCustomizerFactory = other.defaultParameterCustomizerFactory;
    }

    @Override
    public SqlObjectStatementCustomizerConfiguration createCopy() {
        return new SqlObjectStatementCustomizerConfiguration(this);
    }

    /**
     * Returns the configured {@link SqlStatementCustomizerFactory} used to bind sql statement parameters
     * when parameter is not explicitly annotated. By default it is configured as an instance of {@link Bind.Factory}.
     *
     * @return the configured {@link SqlStatementCustomizerFactory}.
     */
    public SqlStatementCustomizerFactory getDefaultParameterCustomizerFactory() {
        return defaultParameterCustomizerFactory;
    }

    /**
     * Configures SqlObject to use the given default bind annotation.
     *
     * @param defaultParameterCustomizerFactory the new default bind annotation.
     * @return this {@link SqlObjectStatementCustomizerConfiguration}.
     */
    public SqlObjectStatementCustomizerConfiguration setDefaultParameterCustomizerFactory(SqlStatementCustomizerFactory defaultParameterCustomizerFactory) {
        this.defaultParameterCustomizerFactory = defaultParameterCustomizerFactory;
        return this;
    }

}
