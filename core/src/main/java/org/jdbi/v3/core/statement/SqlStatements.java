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

import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jdbi.v3.core.config.JdbiConfig;

/**
 * Configuration holder for {@link SqlStatement}s.
 */
public final class SqlStatements implements JdbiConfig<SqlStatements> {

    private final Map<String, Object> attributes;
    private TemplateEngine templateEngine;
    private SqlParser sqlParser;
    private SqlLogger sqlLogger;

    public SqlStatements() {
        attributes = new ConcurrentHashMap<>();
        templateEngine = new DefinedAttributeTemplateEngine();
        sqlParser = new ColonPrefixSqlParser();
        sqlLogger = SqlLogger.NOP_SQL_LOGGER;
    }

    private SqlStatements(SqlStatements that) {
        this.attributes = new ConcurrentHashMap<>(that.attributes);
        this.templateEngine = that.templateEngine;
        this.sqlParser = that.sqlParser;
        this.sqlLogger = that.sqlLogger;
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

    @Override
    public SqlStatements createCopy() {
        return new SqlStatements(this);
    }
}
