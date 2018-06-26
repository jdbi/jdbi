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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.meta.Beta;

/**
 * Configuration holder for {@link SqlStatement}s.
 */
public final class SqlStatements implements JdbiConfig<SqlStatements> {

    private final Map<String, Object> attributes;
    private TemplateEngine templateEngine;
    private SqlParser sqlParser;
    private SqlLogger sqlLogger;
    private Integer queryTimeout;
    private boolean allowUnusedBindings;

    public SqlStatements() {
        attributes = new ConcurrentHashMap<>();
        templateEngine = new DefinedAttributeTemplateEngine();
        sqlParser = new ColonPrefixSqlParser();
        sqlLogger = SqlLogger.NOP_SQL_LOGGER;
        queryTimeout = null;
    }

    private SqlStatements(SqlStatements that) {
        this.attributes = new ConcurrentHashMap<>(that.attributes);
        this.templateEngine = that.templateEngine;
        this.sqlParser = that.sqlParser;
        this.sqlLogger = that.sqlLogger;
        this.queryTimeout = that.queryTimeout;
        allowUnusedBindings = that.allowUnusedBindings;
    }

    /**
     * Define an attribute for {@link StatementContext} for statements executed by Jdbi.
     *
     * @param key   the key for the attribute
     * @param value the value for the attribute
     * @return this
     */
    public SqlStatements define(String key, Object value) {
        if (value == null) {
            attributes.remove(key);
        } else {
            attributes.put(key, value);
        }
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
        return attributes;
    }

    /**
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
     *
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
     * @deprecated use {@link #setSqlLogger} instead
     * @param timingCollector the new timing collector
     * @return this
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

    public SqlLogger getSqlLogger() {
        return sqlLogger;
    }

    public SqlStatements setSqlLogger(SqlLogger sqlLogger) {
        this.sqlLogger = sqlLogger == null ? SqlLogger.NOP_SQL_LOGGER : sqlLogger;
        return this;
    }

    @Beta
    public Integer getQueryTimeout() {
        return queryTimeout;
    }

    /**
     * Jdbi does not implement its own timeout mechanism: it simply calls {@link java.sql.Statement#setQueryTimeout}, leaving timeout handling to your jdbc driver.
     *
     * @param seconds the time in seconds to wait for a query to complete; 0 to disable the timeout; null to leave it at defaults (i.e. Jdbi will not call {@code setQueryTimeout(int)})
     */
    @Beta
    public SqlStatements setQueryTimeout(@Nullable Integer seconds) {
        if (seconds != null && seconds < 0) {
            throw new IllegalArgumentException("queryTimeout must not be < 0");
        }
        this.queryTimeout = seconds;
        return this;
    }

    public boolean getAllowUnusedBindings() {
        return allowUnusedBindings;
    }

    /**
     * Sets whether or not an exception should be thrown when any arguments are given to a query but not actually used in it. Unused bindings tend to be bugs or oversights, but can also just be convenient. Defaults to false: unused bindings are not allowed.
     *
     * @see org.jdbi.v3.core.argument.Argument
     */
    public SqlStatements setAllowUnusedBindings(boolean allowUnusedBindings) {
        this.allowUnusedBindings = allowUnusedBindings;
        return this;
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
}
