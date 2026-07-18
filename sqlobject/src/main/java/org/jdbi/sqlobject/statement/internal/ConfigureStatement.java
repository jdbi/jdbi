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
package org.jdbi.sqlobject.statement.internal;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.statement.Binding;
import org.jdbi.core.statement.QueryCustomizerMixin;
import org.jdbi.core.statement.SqlStatements;
import org.jdbi.core.statement.StatementContext;
import org.jdbi.core.statement.StatementCustomizer;
import org.jdbi.core.statement.StatementCustomizers;

/**
 * The build-time surface that configure-phase customizers are applied to when a {@link
 * org.jdbi.core.statement.QueryTemplate} is built for a SQL Object method. Their effects land on the
 * template's method-level configuration once: configuration mutations and registered statement
 * customizers are frozen into the shared snapshot, and every execution of the template inherits them.
 *
 * <p>Only configuration operations are supported here. Binding a parameter or reading the statement
 * context has no meaning before any invocation, so those throw; such work belongs to a bind-phase
 * (parameter) customizer instead.
 */
final class ConfigureStatement implements QueryCustomizerMixin<ConfigureStatement> {

    private final ConfigRegistry config;

    ConfigureStatement(final ConfigRegistry config) {
        this.config = config;
    }

    @Override
    public ConfigRegistry getConfig() {
        return config;
    }

    @Override
    public ConfigureStatement addCustomizer(final StatementCustomizer customizer) {
        config.configure(SqlStatements.class, c -> c.addCustomizer(customizer));
        return this;
    }

    @Override
    public ConfigureStatement setQueryTimeout(final int seconds) {
        return addCustomizer(StatementCustomizers.statementTimeout(seconds));
    }

    @Override
    public ConfigureStatement define(final String key, final Object value) {
        config.configure(SqlStatements.class, c -> c.define(key, value));
        return this;
    }

    @Override
    public Binding getBinding() {
        throw new UnsupportedOperationException(
            "A configure-phase customizer cannot bind parameters; move binding work to a parameter customizer.");
    }

    @Override
    public StatementContext getContext() {
        throw new UnsupportedOperationException(
            "A configure-phase customizer has no statement context; it runs once when the template is built.");
    }

    @Override
    public ConfigureStatement attachToHandleForCleanup() {
        throw new UnsupportedOperationException(
            "A configure-phase customizer has no handle; it runs once when the template is built.");
    }
}
