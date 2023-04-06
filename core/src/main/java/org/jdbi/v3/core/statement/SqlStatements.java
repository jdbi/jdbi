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

import java.sql.SQLException;
import java.sql.Statement;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.cache.JdbiCache;
import org.jdbi.v3.core.cache.JdbiCacheBuilder;
import org.jdbi.v3.core.cache.JdbiCacheLoader;
import org.jdbi.v3.core.cache.internal.DefaultJdbiCacheBuilder;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.meta.Beta;

/**
 * Configuration holder for {@link SqlStatement}s.
 */
public final class SqlStatements implements JdbiConfig<SqlStatements> {

    /** The default size of the SQL template cache. */
    public static final int SQL_TEMPLATE_CACHE_SIZE = 1_000;

    private final Map<String, Object> attributes;
    private TemplateEngine templateEngine;
    private JdbiCache<StatementCacheKey, Function<StatementContext, String>> templateCache;
    private SqlParser sqlParser;
    private SqlLogger sqlLogger;
    private Integer queryTimeout;
    private boolean allowUnusedBindings;
    private boolean attachAllStatementsForCleanup;
    private boolean attachCallbackStatementsForCleanup = true;
    private final Collection<StatementCustomizer> customizers = new CopyOnWriteArrayList<>();

    private final Collection<StatementContextListener> contextListeners = new CopyOnWriteArraySet<>();

    public SqlStatements() {
        attributes = Collections.synchronizedMap(new HashMap<>());
        templateEngine = new DefinedAttributeTemplateEngine();
        sqlParser = new ColonPrefixSqlParser();
        sqlLogger = SqlLogger.NOP_SQL_LOGGER;
        queryTimeout = null;
        templateCache = DefaultJdbiCacheBuilder.builder().maxSize(SQL_TEMPLATE_CACHE_SIZE).build();
    }

    private SqlStatements(SqlStatements that) {
        this.attributes = Collections.synchronizedMap(that.getAttributes()); // already copied
        this.templateEngine = that.templateEngine;
        this.sqlParser = that.sqlParser;
        this.sqlLogger = that.sqlLogger;
        this.queryTimeout = that.queryTimeout;
        this.allowUnusedBindings = that.allowUnusedBindings;
        this.attachAllStatementsForCleanup = that.attachAllStatementsForCleanup;
        this.attachCallbackStatementsForCleanup = that.attachCallbackStatementsForCleanup;
        this.customizers.addAll(that.customizers);
        this.contextListeners.addAll(that.contextListeners);
        this.templateCache = that.templateCache;
    }

    /**
     * Define an attribute for {@link StatementContext} for statements executed by Jdbi.
     *
     * @param key   the key for the attribute
     * @param value the value for the attribute
     * @return this
     */
    public SqlStatements define(String key, Object value) {
        attributes.put(key, value);
        return this;
    }

    /**
     * Defines attributes for each key/value pair in the Map.
     *
     * @param values map of attributes to define.
     * @return this
     */
    public SqlStatements defineMap(final Map<String, ?> values) {
        if (values != null) {
            attributes.putAll(values);
        }
        return this;
    }

    /**
     * Obtain the value of an attribute
     *
     * @param key the name of the attribute
     * @return the value of the attribute
     */
    public Object getAttribute(String key) {
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
     * @return this
     */
    public SqlStatements addCustomizer(final StatementCustomizer customizer) {
        this.customizers.add(customizer);
        return this;
    }

    public SqlStatements addContextListener(final StatementContextListener listener) {
        this.contextListeners.add(listener);
        return this;
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
     * Sets the {@link TemplateEngine} used to render SQL for all
     * {@link SqlStatement SQL statements} executed by Jdbi. The default
     * engine replaces <code>&lt;name&gt;</code>-style tokens
     * with attributes {@link StatementContext#define(String, Object) defined}
     * on the statement context.
     *
     * @param templateEngine the new template engine.
     * @return this
     */
    public SqlStatements setTemplateEngine(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
        return this;
    }

    /**
     * Sets the cache used to avoid repeatedly parsing SQL statements.
     *
     * @param cacheBuilder the cache builder to use to create the cache.
     * @return this
     */
    @Beta
    public SqlStatements setTemplateCache(JdbiCacheBuilder cacheBuilder) {
        templateCache = cacheBuilder.build();
        return this;
    }

    public SqlParser getSqlParser() {
        return sqlParser;
    }

    /**
     * Sets the {@link SqlParser} used to parse parameters in SQL statements
     * executed by Jdbi. The default parses colon-prefixed named parameter
     * tokens, e.g. <code>:name</code>.
     *
     * @param sqlParser the new SQL parser.
     * @return this
     */
    public SqlStatements setSqlParser(SqlParser sqlParser) {
        this.sqlParser = sqlParser;
        return this;
    }

    /**
     * @return the timing collector
     * @deprecated use {@link #getSqlLogger} instead
     */
    @Deprecated
    public TimingCollector getTimingCollector() {
        return (elapsed, ctx) -> sqlLogger.logAfterExecution(ctx);
    }

    /**
     * Sets the {@link TimingCollector} used to collect timing about the {@link SqlStatement SQL statements} executed
     * by Jdbi. The default collector does nothing.
     *
     * @param timingCollector the new timing collector
     * @return this
     * @deprecated use {@link #setSqlLogger} instead
     */
    @Deprecated
    public SqlStatements setTimingCollector(TimingCollector timingCollector) {
        this.sqlLogger = timingCollector == null ? SqlLogger.NOP_SQL_LOGGER : new SqlLogger() {
            @Override
            public void logAfterExecution(StatementContext context) {
                timingCollector.collect(context.getElapsedTime(ChronoUnit.NANOS), context);
            }
        };
        return this;
    }

    /**
     * Returns the current logger.
     * @return A {@link SqlLogger} instance
     */
    public SqlLogger getSqlLogger() {
        return sqlLogger;
    }

    /**
     * Sets a {@link SqlLogger} instance to log all SQL operations.
     * @param sqlLogger The logger. Using <code>null</code> turns off all logging
     * @return this
     */
    public SqlStatements setSqlLogger(SqlLogger sqlLogger) {
        this.sqlLogger = sqlLogger == null ? SqlLogger.NOP_SQL_LOGGER : sqlLogger;
        return this;
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
     * @return this
     */
    @Beta
    public SqlStatements setQueryTimeout(@Nullable Integer seconds) {
        if (seconds != null && seconds < 0) {
            throw new IllegalArgumentException("queryTimeout must not be < 0");
        }
        this.queryTimeout = seconds;
        return this;
    }

    public boolean isUnusedBindingAllowed() {
        return allowUnusedBindings;
    }

    /**
     * Sets whether or not an exception should be thrown when any arguments are given to a query but not actually used in it.
     * Unused bindings tend to be bugs or oversights, but are not always.
     * Defaults to false: unused bindings are not allowed.
     *
     * @param unusedBindingAllowed the new setting
     * @return this
     * @see org.jdbi.v3.core.argument.Argument
     */
    public SqlStatements setUnusedBindingAllowed(boolean unusedBindingAllowed) {
        this.allowUnusedBindings = unusedBindingAllowed;
        return this;
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
     * Sets whether all statements created will automatically attached to the corresponding {@link Handle} object automatically.
     * This can be useful when mostly short-lived handles are used because closing the handle will now clean up all outstanding resources from
     * any statement. The default is false.
     *
     * @param attachAllStatementsForCleanup If true, all statements are automatically attached to the Handle
     *
     * @since 3.38.0
     */
    @Beta
    public void setAttachAllStatementsForCleanup(boolean attachAllStatementsForCleanup) {
        this.attachAllStatementsForCleanup = attachAllStatementsForCleanup;
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
     * Sets whether statements created within the {@link Jdbi#withHandle}, {@link Jdbi#useHandle}, {@link Jdbi#inTransaction} and {@link Jdbi#useTransaction}
     * callback methods will automatically attached to the {@link Handle} object and therefore cleaned up when the callback ends. The default is true.

     * @param attachCallbackStatementsForCleanup If true, statements created within the Jdbi callbacks are attached to the handle
     *
     * @since 3.38.0
     */
    public void setAttachCallbackStatementsForCleanup(boolean attachCallbackStatementsForCleanup) {
        this.attachCallbackStatementsForCleanup = attachCallbackStatementsForCleanup;
    }

    /**
     * Returns cache statistics for the internal template cache. This returns a cache specific object,
     * so the user needs to know what caching library is in use.
     */
    @Beta
    public <T> T cacheStats() {
        return templateCache.getStats();
    }


    void customize(Statement statement) throws SQLException {
        if (queryTimeout != null) {
            statement.setQueryTimeout(queryTimeout);
        }
    }

    @Override
    public SqlStatements createCopy() {
        return new SqlStatements(this);
    }

    Collection<StatementCustomizer> getCustomizers() {
        return customizers;
    }

    Collection<StatementContextListener> getContextListeners() {
        return contextListeners;
    }

    String preparedRender(String template, StatementContext ctx) {
        try {
            return Optional.ofNullable(
                            templateCache.getWithLoader(
                                    new StatementCacheKey(templateEngine, template),
                                    cacheLoaderFunction(ctx)))
                    .orElse(cx -> templateEngine.render(template, cx)) // fall-back to old behavior
                    .apply(ctx);
        } catch (final IllegalArgumentException e) {
            throw new UnableToCreateStatementException("Exception rendering SQL template", e, ctx);
        }
    }

    private static JdbiCacheLoader<StatementCacheKey, Function<StatementContext, String>> cacheLoaderFunction(StatementContext ctx) {
        return key -> key.getTemplateEngine().parse(key.getTemplate(), ctx.getConfig()).orElse(null);
    }

    private static final class StatementCacheKey {

        private final TemplateEngine templateEngine;
        private final String template;

        StatementCacheKey(TemplateEngine templateEngine, String template) {
            this.templateEngine = templateEngine;
            this.template = template;
        }

        @SuppressWarnings("PMD.UnusedPrivateMethod") // PMD gets that one wrong...
        private TemplateEngine getTemplateEngine() {
            return templateEngine;
        }

        private String getTemplate() {
            return template;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            StatementCacheKey that = (StatementCacheKey) o;
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
