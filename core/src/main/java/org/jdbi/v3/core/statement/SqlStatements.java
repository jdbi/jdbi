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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.rewriter.ColonPrefixStatementParser;
import org.jdbi.v3.core.rewriter.DefinedAttributeRewriter;
import org.jdbi.v3.core.rewriter.StatementParser;
import org.jdbi.v3.core.rewriter.StatementRewriter;

public final class SqlStatements implements JdbiConfig<SqlStatements> {

    private final Map<String, Object> attributes;
    private StatementRewriter statementRewriter;
    private StatementParser statementParser;
    private TimingCollector timingCollector;

    public SqlStatements() {
        attributes = new ConcurrentHashMap<>();
        statementRewriter = new DefinedAttributeRewriter();
        statementParser = new ColonPrefixStatementParser();
        timingCollector = TimingCollector.NOP_TIMING_COLLECTOR;
    }

    private SqlStatements(SqlStatements that) {
        this.attributes = new ConcurrentHashMap<>(that.attributes);
        this.statementRewriter = that.statementRewriter;
        this.statementParser = that.statementParser;
        this.timingCollector = that.timingCollector;
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

    public StatementRewriter getStatementRewriter() {
        return statementRewriter;
    }

    /**
     * Sets the {@link StatementRewriter} used to rewrite SQL for all
     * {@link SqlStatement SQL statements} executed by Jdbi. The default
     * statement rewriter replaces <code>&lt;name&gt;</code>-style tokens
     * with attributes {@link StatementContext#define(String, Object) defined}
     * on the statement context.
     *
     * @param rewriter the new statement rewriter.
     * @return this
     */
    public SqlStatements setStatementRewriter(StatementRewriter rewriter) {
        this.statementRewriter = rewriter;
        return this;
    }

    public StatementParser getStatementParser() {
        return statementParser;
    }

    /**
     * Sets the {@link StatementParser} used to parse parameters in SQL
     * statements executed by Jdbi. The default parser colon prefixed named
     * parameter tokens, e.g. <code>:name</code>.
     *
     * @param statementParser the new statement parser.
     * @return this
     */
    public SqlStatements setStatementParser(StatementParser statementParser) {
        this.statementParser = statementParser;
        return this;
    }

    public TimingCollector getTimingCollector() {
        return timingCollector;
    }

    /**
     * Sets the {@link TimingCollector} used to collect timing about the {@link SqlStatement SQL statements} executed
     * by Jdbi. The default collector does nothing.
     *
     * @param timingCollector the new timing collector
     * @return this
     */
    public SqlStatements setTimingCollector(TimingCollector timingCollector) {
        this.timingCollector = timingCollector == null ? TimingCollector.NOP_TIMING_COLLECTOR : timingCollector;
        return this;
    }

    @Override
    public SqlStatements createCopy() {
        return new SqlStatements(this);
    }
}
