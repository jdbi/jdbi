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
package org.jdbi.v3.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jdbi.v3.core.rewriter.ColonPrefixStatementRewriter;
import org.jdbi.v3.core.rewriter.StatementRewriter;

public final class SqlStatementConfig implements JdbiConfig<SqlStatementConfig> {

    private final Map<String, Object> attributes;
    private volatile StatementRewriter statementRewriter;
    private volatile TimingCollector timingCollector;

    public SqlStatementConfig() {
        attributes = new ConcurrentHashMap<>();
        statementRewriter = new ColonPrefixStatementRewriter();
        timingCollector = TimingCollector.NOP_TIMING_COLLECTOR;
    }

    private SqlStatementConfig(SqlStatementConfig that) {
        this.attributes = new ConcurrentHashMap<>(that.attributes);
        this.statementRewriter = that.statementRewriter;
        this.timingCollector = that.timingCollector;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public StatementRewriter getStatementRewriter() {
        return statementRewriter;
    }

    public void setStatementRewriter(StatementRewriter statementRewriter) {
        this.statementRewriter = statementRewriter;
    }

    public TimingCollector getTimingCollector() {
        return timingCollector;
    }

    public void setTimingCollector(TimingCollector timingCollector) {
        this.timingCollector = timingCollector == null ? TimingCollector.NOP_TIMING_COLLECTOR : timingCollector;
    }

    @Override
    public SqlStatementConfig createChild() {
        return new SqlStatementConfig(this);
    }
}
