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
package org.jdbi.core.statement;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;

import jakarta.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.cache.JdbiCache;
import org.jdbi.core.cache.JdbiCacheBuilder;
import org.jdbi.core.cache.JdbiCacheLoader;
import org.jdbi.core.cache.internal.DefaultJdbiCacheBuilder;
import org.jdbi.core.config.JdbiConfig;
import org.jdbi.core.internal.RegistrationLists;
import org.jdbi.core.internal.exceptions.Sneaky;
import org.jdbi.meta.Beta;

/**
 * Configuration holder for {@link SqlStatement}s.
 * <p>
 * This configuration is immutable: the {@code define}, {@code addX} registration methods and the policy withers
 * each return a new instance, leaving the receiver unchanged. The single {@link #templateCache} instance is shared
 * across derived instances so it stays warm; only {@link #templateCache(JdbiCacheBuilder)} installs a fresh cache.
 */
public final class SqlStatements implements JdbiConfig<SqlStatements> {

    /** The default size of the SQL template cache. */
    public static final int SQL_TEMPLATE_CACHE_SIZE = 1_000;

    private final Map<String, Object> attributes;
    private final TemplateEngine templateEngine;
    private final JdbiCache<StatementCacheKey, Function<RenderContext, String>> templateCache;
    private final SqlParser sqlParser;
    private final SqlLogger sqlLogger;
    private final Integer queryTimeout;
    private final boolean allowUnusedBindings;
    private final boolean attachAllStatementsForCleanup;
    private final boolean attachCallbackStatementsForCleanup;
    private final boolean scriptStatementsNeedSemicolon;
    // Applied in registration order.
    private final List<StatementCustomizer> customizers;
    // Consulted latest-registered first (see handleException).
    private final List<SqlExceptionHandler> exceptionHandlers;
    // Insertion order, no duplicates.
    private final List<StatementContextListener> contextListeners;

    // Don't emit unlimited amounts of data via telemetry
    private final int jfrSqlMaxLength;
    private final int jfrParamMaxLength;

    private final boolean includeBindingsInTelemetry;

    public SqlStatements() {
        this(Map.of(),
                new DefinedAttributeTemplateEngine(),
                DefaultJdbiCacheBuilder.builder().maxSize(SQL_TEMPLATE_CACHE_SIZE).build(),
                new ColonPrefixSqlParser(),
                SqlLogger.NOP_SQL_LOGGER,
                null,
                false,
                false,
                true,
                true,
                List.of(),
                List.of(),
                List.of(),
                512,
                512,
                true);
    }

    private SqlStatements(final Map<String, Object> attributes,
            final TemplateEngine templateEngine,
            final JdbiCache<StatementCacheKey, Function<RenderContext, String>> templateCache,
            final SqlParser sqlParser,
            final SqlLogger sqlLogger,
            @Nullable final Integer queryTimeout,
            final boolean allowUnusedBindings,
            final boolean attachAllStatementsForCleanup,
            final boolean attachCallbackStatementsForCleanup,
            final boolean scriptStatementsNeedSemicolon,
            final List<StatementCustomizer> customizers,
            final List<SqlExceptionHandler> exceptionHandlers,
            final List<StatementContextListener> contextListeners,
            final int jfrSqlMaxLength,
            final int jfrParamMaxLength,
            final boolean includeBindingsInTelemetry) {
        this.attributes = attributes;
        this.templateEngine = templateEngine;
        this.templateCache = templateCache;
        this.sqlParser = sqlParser;
        this.sqlLogger = sqlLogger;
        this.queryTimeout = queryTimeout;
        this.allowUnusedBindings = allowUnusedBindings;
        this.attachAllStatementsForCleanup = attachAllStatementsForCleanup;
        this.attachCallbackStatementsForCleanup = attachCallbackStatementsForCleanup;
        this.scriptStatementsNeedSemicolon = scriptStatementsNeedSemicolon;
        this.customizers = customizers;
        this.exceptionHandlers = exceptionHandlers;
        this.contextListeners = contextListeners;
        this.jfrSqlMaxLength = jfrSqlMaxLength;
        this.jfrParamMaxLength = jfrParamMaxLength;
        this.includeBindingsInTelemetry = includeBindingsInTelemetry;
    }

    /**
     * Define an attribute for {@link StatementContext} for statements executed by Jdbi.
     *
     * @param key   the key for the attribute
     * @param value the value for the attribute
     * @return a copy of this configuration with the attribute defined
     */
    @CheckReturnValue
    public SqlStatements define(final String key, final Object value) {
        final Map<String, Object> newAttributes = new HashMap<>(attributes);
        newAttributes.put(key, value);
        return withAttributes(newAttributes);
    }

    /**
     * Defines attributes for each key/value pair in the Map.
     *
     * @param values map of attributes to define.
     * @return a copy of this configuration with the attributes defined, or this configuration if {@code values} is null or empty
     */
    @CheckReturnValue
    public SqlStatements defineMap(final Map<String, ?> values) {
        if (values == null || values.isEmpty()) {
            return this;
        }
        final Map<String, Object> newAttributes = new HashMap<>(attributes);
        newAttributes.putAll(values);
        return withAttributes(newAttributes);
    }

    // newAttributes is a freshly-built map owned solely by this call, so wrapping (rather than copying again) is safe.
    // Collections.unmodifiableMap tolerates null values, which HashMap-backed defines historically allowed.
    private SqlStatements withAttributes(final Map<String, Object> newAttributes) {
        return new SqlStatements(Collections.unmodifiableMap(newAttributes), templateEngine, templateCache, sqlParser, sqlLogger,
                queryTimeout, allowUnusedBindings, attachAllStatementsForCleanup, attachCallbackStatementsForCleanup,
                scriptStatementsNeedSemicolon, customizers, exceptionHandlers, contextListeners,
                jfrSqlMaxLength, jfrParamMaxLength, includeBindingsInTelemetry);
    }

    /**
     * Obtain the value of an attribute
     *
     * @param key the name of the attribute
     * @return the value of the attribute
     */
    public Object getAttribute(final String key) {
        return attributes.get(key);
    }

    /**
     * Returns the attributes which will be applied to {@link SqlStatement SQL statements} created by Jdbi.
     *
     * @return the defined attributes.
     */
    public Map<String, Object> getAttributes() {
        return new HashMap<>(attributes);
    }

    /**
     * Provides a means for custom statement modification. Common customizations
     * have their own methods, such as {@link Query#setMaxRows(int)}
     *
     * @param customizer instance to be used to customize a statement
     * @return a copy of this configuration with the customizer registered
     */
    @CheckReturnValue
    public SqlStatements addCustomizer(final StatementCustomizer customizer) {
        return new SqlStatements(attributes, templateEngine, templateCache, sqlParser, sqlLogger,
                queryTimeout, allowUnusedBindings, attachAllStatementsForCleanup, attachCallbackStatementsForCleanup,
                scriptStatementsNeedSemicolon, RegistrationLists.append(customizers, customizer), exceptionHandlers,
                contextListeners, jfrSqlMaxLength, jfrParamMaxLength, includeBindingsInTelemetry);
    }

    @CheckReturnValue
    public SqlStatements addContextListener(final StatementContextListener listener) {
        return new SqlStatements(attributes, templateEngine, templateCache, sqlParser, sqlLogger,
                queryTimeout, allowUnusedBindings, attachAllStatementsForCleanup, attachCallbackStatementsForCleanup,
                scriptStatementsNeedSemicolon, customizers, exceptionHandlers,
                RegistrationLists.appendDistinct(contextListeners, listener),
                jfrSqlMaxLength, jfrParamMaxLength, includeBindingsInTelemetry);
    }

    /**
     * Returns the {@link TemplateEngine} which renders the SQL template.
     *
     * @return the template engine which renders the SQL template prior to
     * parsing parameters.
     */
    public TemplateEngine getTemplateEngine() {
        return templateEngine;
    }

    /**
     * Returns a copy of this configuration using the given {@link TemplateEngine} to render SQL for all
     * {@link SqlStatement SQL statements} executed by Jdbi. The default
     * engine replaces <code>&lt;name&gt;</code>-style tokens
     * with attributes {@link StatementContext#define(String, Object) defined}
     * on the statement context.
     *
     * @param templateEngine the new template engine.
     * @return the derived configuration
     */
    @CheckReturnValue
    public SqlStatements templateEngine(final TemplateEngine templateEngine) {
        return new SqlStatements(attributes, templateEngine, templateCache, sqlParser, sqlLogger,
                queryTimeout, allowUnusedBindings, attachAllStatementsForCleanup, attachCallbackStatementsForCleanup,
                scriptStatementsNeedSemicolon, customizers, exceptionHandlers, contextListeners,
                jfrSqlMaxLength, jfrParamMaxLength, includeBindingsInTelemetry);
    }

    /**
     * Returns a copy of this configuration using a fresh cache, built from the given builder, to avoid repeatedly
     * parsing SQL statements.
     *
     * @param cacheBuilder the cache builder to use to create the cache.
     * @return the derived configuration
     */
    @CheckReturnValue
    @Beta
    public SqlStatements templateCache(final JdbiCacheBuilder cacheBuilder) {
        return new SqlStatements(attributes, templateEngine, cacheBuilder.build(), sqlParser, sqlLogger,
                queryTimeout, allowUnusedBindings, attachAllStatementsForCleanup, attachCallbackStatementsForCleanup,
                scriptStatementsNeedSemicolon, customizers, exceptionHandlers, contextListeners,
                jfrSqlMaxLength, jfrParamMaxLength, includeBindingsInTelemetry);
    }

    public SqlParser getSqlParser() {
        return sqlParser;
    }

    /**
     * Returns a copy of this configuration using the given {@link SqlParser} to parse parameters in SQL statements
     * executed by Jdbi. The default parses colon-prefixed named parameter
     * tokens, e.g. <code>:name</code>.
     *
     * @param sqlParser the new SQL parser.
     * @return the derived configuration
     */
    @CheckReturnValue
    public SqlStatements sqlParser(final SqlParser sqlParser) {
        return new SqlStatements(attributes, templateEngine, templateCache, sqlParser, sqlLogger,
                queryTimeout, allowUnusedBindings, attachAllStatementsForCleanup, attachCallbackStatementsForCleanup,
                scriptStatementsNeedSemicolon, customizers, exceptionHandlers, contextListeners,
                jfrSqlMaxLength, jfrParamMaxLength, includeBindingsInTelemetry);
    }

    /**
     * @return the timing collector
     * @deprecated use {@link #getSqlLogger} instead
     */
    @Deprecated(since = "3.2.0", forRemoval = true)
    public TimingCollector getTimingCollector() {
        return (elapsed, ctx) -> sqlLogger.logAfterExecution(ctx);
    }

    /**
     * Returns a copy of this configuration using the given {@link TimingCollector} to collect timing about the
     * {@link SqlStatement SQL statements} executed by Jdbi. The default collector does nothing.
     *
     * @param timingCollector the new timing collector
     * @return the derived configuration
     * @deprecated use {@link #sqlLogger} instead
     */
    @CheckReturnValue
    @Deprecated(since = "3.2.0", forRemoval = true)
    public SqlStatements setTimingCollector(final TimingCollector timingCollector) {
        final SqlLogger logger = timingCollector == null ? SqlLogger.NOP_SQL_LOGGER : new SqlLogger() {
            @Override
            public void logAfterExecution(final StatementContext context) {
                timingCollector.collect(context.getElapsedTime(ChronoUnit.NANOS), context);
            }
        };
        return sqlLogger(logger);
    }

    /**
     * Returns the current logger.
     * @return A {@link SqlLogger} instance
     */
    public SqlLogger getSqlLogger() {
        return sqlLogger;
    }

    /**
     * Returns a copy of this configuration using the given {@link SqlLogger} to log all SQL operations.
     * @param sqlLogger The logger. Using <code>null</code> turns off all logging
     * @return the derived configuration
     */
    @CheckReturnValue
    public SqlStatements sqlLogger(final SqlLogger sqlLogger) {
        return new SqlStatements(attributes, templateEngine, templateCache, sqlParser,
                sqlLogger == null ? SqlLogger.NOP_SQL_LOGGER : sqlLogger,
                queryTimeout, allowUnusedBindings, attachAllStatementsForCleanup, attachCallbackStatementsForCleanup,
                scriptStatementsNeedSemicolon, customizers, exceptionHandlers, contextListeners,
                jfrSqlMaxLength, jfrParamMaxLength, includeBindingsInTelemetry);
    }

    @Beta
    public Integer getQueryTimeout() {
        return queryTimeout;
    }

    /**
     * Jdbi does not implement its own timeout mechanism: it simply calls {@link java.sql.Statement#setQueryTimeout}, leaving timeout handling to your jdbc
     * driver.
     *
     * @param seconds the time in seconds to wait for a query to complete; 0 to disable the timeout; null to leave it at defaults (i.e. Jdbi will not call
     *                {@code setQueryTimeout(int)})
     * @return the derived configuration
     */
    @CheckReturnValue
    @Beta
    public SqlStatements queryTimeout(@Nullable final Integer seconds) {
        if (seconds != null && seconds < 0) {
            throw new IllegalArgumentException("queryTimeout must not be < 0");
        }
        return new SqlStatements(attributes, templateEngine, templateCache, sqlParser, sqlLogger,
                seconds, allowUnusedBindings, attachAllStatementsForCleanup, attachCallbackStatementsForCleanup,
                scriptStatementsNeedSemicolon, customizers, exceptionHandlers, contextListeners,
                jfrSqlMaxLength, jfrParamMaxLength, includeBindingsInTelemetry);
    }

    public boolean isUnusedBindingAllowed() {
        return allowUnusedBindings;
    }

    /**
     * Returns a copy of this configuration setting whether or not an exception should be thrown when any arguments are given to a query but not actually used
     * in it. Unused bindings tend to be bugs or oversights, but are not always.
     * Defaults to false: unused bindings are not allowed.
     *
     * @param unusedBindingAllowed the new setting
     * @return the derived configuration
     * @see org.jdbi.core.argument.Argument
     */
    @CheckReturnValue
    public SqlStatements unusedBindingAllowed(final boolean unusedBindingAllowed) {
        return new SqlStatements(attributes, templateEngine, templateCache, sqlParser, sqlLogger,
                queryTimeout, unusedBindingAllowed, attachAllStatementsForCleanup, attachCallbackStatementsForCleanup,
                scriptStatementsNeedSemicolon, customizers, exceptionHandlers, contextListeners,
                jfrSqlMaxLength, jfrParamMaxLength, includeBindingsInTelemetry);
    }

    /**
     * If true, all statements created within {@link Jdbi#withHandle}, {@link Jdbi#useHandle}, {@link Jdbi#inTransaction} and {@link Jdbi#useTransaction}
     * are attached to the {@link Handle} object for cleanup.
     *
     * @return True if statements are attached to their handle for cleanup
     *
     * @since 3.38.0
     */
    public boolean isAttachAllStatementsForCleanup() {
        return attachAllStatementsForCleanup;
    }

    /**
     * Returns a copy of this configuration setting whether all statements created will automatically attached to the corresponding {@link Handle} object
     * automatically. This can be useful when mostly short-lived handles are used because closing the handle will now clean up all outstanding resources from
     * any statement. The default is false.
     *
     * @param attachAllStatementsForCleanup If true, all statements are automatically attached to the Handle
     * @return the derived configuration
     *
     * @since 3.38.0
     */
    @CheckReturnValue
    @Beta
    public SqlStatements attachAllStatementsForCleanup(final boolean attachAllStatementsForCleanup) {
        return new SqlStatements(attributes, templateEngine, templateCache, sqlParser, sqlLogger,
                queryTimeout, allowUnusedBindings, attachAllStatementsForCleanup, attachCallbackStatementsForCleanup,
                scriptStatementsNeedSemicolon, customizers, exceptionHandlers, contextListeners,
                jfrSqlMaxLength, jfrParamMaxLength, includeBindingsInTelemetry);
    }

    /**
     * If true, statements created within {@link Jdbi#withHandle}, {@link Jdbi#useHandle}, {@link Jdbi#inTransaction} and {@link Jdbi#useTransaction}
     * will be attached to the {@link Handle} object in the callback for cleanup.
     *
     * @return True if statements are attached to their handle within Jdbi callbacks
     *
     * @since 3.38.0
     */
    public boolean isAttachCallbackStatementsForCleanup() {
        return attachCallbackStatementsForCleanup;
    }

    /**
     * If true, script statements parsed by a {@link Script} object will have a trailing semicolon. Some
     * databases (e.g. Oracle) require that trailing semicolon while others (e.g. MySQL) do not and may consider
     * it a syntax error. If executing a script against the database results in syntax errors that point at semicolons,
     * change the value of this setting.
     * <br>
     * The default setting is {@code true} for historical reasons.
     *
     * @param scriptStatementsNeedSemicolon If true, parsed statements will have a trailing semicolon.
     * @return the derived configuration
     *
     * @since 3.43.0
     */
    @CheckReturnValue
    public SqlStatements scriptStatementsNeedSemicolon(final boolean scriptStatementsNeedSemicolon) {
        return new SqlStatements(attributes, templateEngine, templateCache, sqlParser, sqlLogger,
                queryTimeout, allowUnusedBindings, attachAllStatementsForCleanup, attachCallbackStatementsForCleanup,
                scriptStatementsNeedSemicolon, customizers, exceptionHandlers, contextListeners,
                jfrSqlMaxLength, jfrParamMaxLength, includeBindingsInTelemetry);
    }

    /**
     * If true, script statements parsed by a {@link Script} object will have a trailing semicolon. Some
     * databases (e.g. Oracle) require that trailing semicolon while others (e.g. MySQL) do not and may consider
     * it a syntax error. If executing a script against the database results in syntax errors that point at semicolons,
     * change the value of this setting.
     *
     * @return True if parsed statements will have a trailing semicolon.
     *
     * @since 3.43.0
     */
    public boolean isScriptStatementsNeedSemicolon() {
        return scriptStatementsNeedSemicolon;
    }

    /**
     * Returns a copy of this configuration setting whether statements created within the {@link Jdbi#withHandle}, {@link Jdbi#useHandle},
     * {@link Jdbi#inTransaction} and {@link Jdbi#useTransaction} callback methods will automatically attached to the {@link Handle} object and therefore
     * cleaned up when the callback ends. The default is true.
     *
     * @param attachCallbackStatementsForCleanup If true, statements created within the Jdbi callbacks are attached to the handle
     * @return the derived configuration
     *
     * @since 3.38.0
     */
    @CheckReturnValue
    public SqlStatements attachCallbackStatementsForCleanup(final boolean attachCallbackStatementsForCleanup) {
        return new SqlStatements(attributes, templateEngine, templateCache, sqlParser, sqlLogger,
                queryTimeout, allowUnusedBindings, attachAllStatementsForCleanup, attachCallbackStatementsForCleanup,
                scriptStatementsNeedSemicolon, customizers, exceptionHandlers, contextListeners,
                jfrSqlMaxLength, jfrParamMaxLength, includeBindingsInTelemetry);
    }

    /**
     * When recording JFR events, the maximum length of rendered SQL to store in the event record.
     *
     * @param jfrSqlMaxLength the maximum length of rendered SQL
     * @return the derived configuration
     */
    @CheckReturnValue
    @Beta
    public SqlStatements jfrSqlMaxLength(final int jfrSqlMaxLength) {
        return new SqlStatements(attributes, templateEngine, templateCache, sqlParser, sqlLogger,
                queryTimeout, allowUnusedBindings, attachAllStatementsForCleanup, attachCallbackStatementsForCleanup,
                scriptStatementsNeedSemicolon, customizers, exceptionHandlers, contextListeners,
                jfrSqlMaxLength, jfrParamMaxLength, includeBindingsInTelemetry);
    }

    /**
     * When recording JFR events, the maximum length of rendered SQL to store in the event record.
     */
    @Beta
    public int getJfrSqlMaxLength() {
        return jfrSqlMaxLength;
    }

    /**
     * Toggle whether to include potentially sensitive bindins in telemetry data.
     */
    @Beta
    public boolean getIncludeBindingsInTelemetry() {
        return includeBindingsInTelemetry;
    }

    /**
     * When recording JFR events, the maximum length of rendered parameters to store in the event record.
     *
     * @param jfrParamMaxLength the maximum length of rendered parameters
     * @return the derived configuration
     */
    @CheckReturnValue
    @Beta
    public SqlStatements jfrParamMaxLength(final int jfrParamMaxLength) {
        return new SqlStatements(attributes, templateEngine, templateCache, sqlParser, sqlLogger,
                queryTimeout, allowUnusedBindings, attachAllStatementsForCleanup, attachCallbackStatementsForCleanup,
                scriptStatementsNeedSemicolon, customizers, exceptionHandlers, contextListeners,
                jfrSqlMaxLength, jfrParamMaxLength, includeBindingsInTelemetry);
    }

    /**
     * When recording JFR events, the maximum length of rendered parameters to store in the event record.
     */
    @Beta
    public int getJfrParamMaxLength() {
        return jfrParamMaxLength;
    }

    /**
     * Returns a copy of this configuration toggling whether to include potentially sensitive bindins in telemetry data.
     *
     * @param includeBindingsInTelemetry whether to include bindings in telemetry data
     * @return the derived configuration
     */
    @CheckReturnValue
    public SqlStatements includeBindingsInTelemetry(final boolean includeBindingsInTelemetry) {
        return new SqlStatements(attributes, templateEngine, templateCache, sqlParser, sqlLogger,
                queryTimeout, allowUnusedBindings, attachAllStatementsForCleanup, attachCallbackStatementsForCleanup,
                scriptStatementsNeedSemicolon, customizers, exceptionHandlers, contextListeners,
                jfrSqlMaxLength, jfrParamMaxLength, includeBindingsInTelemetry);
    }

    /**
     * Add a callback used when statement execution throws SQLException.
     * Latest registered callbacks are fired first.
     *
     * @param handler the handler to register
     * @return a copy of this configuration with the handler registered
     */
    @CheckReturnValue
    @Beta
    public SqlStatements addExceptionHandler(final SqlExceptionHandler handler) {
        return new SqlStatements(attributes, templateEngine, templateCache, sqlParser, sqlLogger,
                queryTimeout, allowUnusedBindings, attachAllStatementsForCleanup, attachCallbackStatementsForCleanup,
                scriptStatementsNeedSemicolon, customizers, RegistrationLists.append(exceptionHandlers, handler),
                contextListeners, jfrSqlMaxLength, jfrParamMaxLength, includeBindingsInTelemetry);
    }

    /**
     * Returns cache statistics for the internal template cache. This returns a cache specific object,
     * so the user needs to know what caching library is in use.
     *
     * @param <T> the type of the cache statistics object
     */
    @Beta
    public <T> T cacheStats() {
        return templateCache.getStats();
    }


    void customize(final Statement statement) throws SQLException {
        if (queryTimeout != null) {
            statement.setQueryTimeout(queryTimeout);
        }
    }


    Collection<StatementCustomizer> getCustomizers() {
        return customizers;
    }

    Collection<StatementContextListener> getContextListeners() {
        return contextListeners;
    }

    String preparedRender(final String template, final RenderContext renderContext) {
        try {
            return Optional.ofNullable(
                            templateCache.getWithLoader(
                                    new StatementCacheKey(templateEngine, template),
                                    cacheLoaderFunction(renderContext)))
                    .orElse(rc -> templateEngine.render(template, rc)) // fall-back to old behavior
                    .apply(renderContext);
        } catch (final IllegalArgumentException e) {
            throw new UnableToCreateStatementException("Exception rendering SQL template", e);
        }
    }

    UnableToExecuteStatementException handleException(SQLException e, StatementContext ctx) {
        // Latest registered handler is fired first.
        for (int i = exceptionHandlers.size() - 1; i >= 0; i--) {
            Throwable rewritten = exceptionHandlers.get(i).handle(e);
            if (rewritten != null) {
                if (!e.equals(rewritten)) {
                    rewritten.addSuppressed(e);
                }
                throw Sneaky.throwAnyway(rewritten);
            }
        }
        throw new UnableToExecuteStatementException(e, ctx);
    }

    private static JdbiCacheLoader<StatementCacheKey, Function<RenderContext, String>> cacheLoaderFunction(final RenderContext renderContext) {
        return key -> key.getTemplateEngine().parse(key.getTemplate(), renderContext).orElse(null);
    }

    private static final class StatementCacheKey {

        private final TemplateEngine templateEngine;
        private final String template;

        StatementCacheKey(final TemplateEngine templateEngine, final String template) {
            this.templateEngine = templateEngine;
            this.template = template;
        }

        TemplateEngine getTemplateEngine() {
            return templateEngine;
        }

        String getTemplate() {
            return template;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final StatementCacheKey that = (StatementCacheKey) o;
            return templateEngine.equals(that.templateEngine) && template.equals(that.template);
        }

        @Override
        public int hashCode() {
            return Objects.hash(templateEngine, template);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", StatementCacheKey.class.getSimpleName() + "[", "]")
                    .add("templateEngine=" + templateEngine)
                    .add("template='" + template + "'")
                    .toString();
        }
    }
}
