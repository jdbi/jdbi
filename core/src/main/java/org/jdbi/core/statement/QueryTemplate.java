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

import org.jdbi.core.Handle;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.result.ResultIterable;
import org.jdbi.core.result.ResultSetScanner;

/**
 * A reusable, immutable, thread-safe query definition. Built once from a {@link org.jdbi.core.Jdbi}
 * (see {@code Jdbi.buildQueryTemplate}) and executed many times by binding it to a {@link Handle}
 * with {@link #with(Handle)}.
 *
 * <p>The SQL is rendered and parsed a single time when the template is built; the resulting
 * {@link ParsedSql} and the configuration are shared read-only, so each execution only binds its
 * parameters and runs the statement.
 */
public class QueryTemplate<R> {
    final QueryTemplateBuilder builder;
    final ResultSetScanner<ResultIterable<R>> scanner;

    // Rendered and parsed once at build time, then reused for every execution.
    final String renderedSql;
    final ParsedSql parsedSql;

    QueryTemplate(final QueryTemplateBuilder builder, final ResultSetScanner<ResultIterable<R>> scanner) {
        this.builder = builder;
        this.scanner = scanner;

        final ConfigRegistry config = builder.getConfig();
        final SqlStatements stmtConfig = config.get(SqlStatements.class);
        this.renderedSql = stmtConfig.preparedRender(builder.getSql(), RenderContext.of(config));
        // The parser uses the context only for exception reporting; parsing depends solely on the SQL.
        this.parsedSql = stmtConfig.getSqlParser()
            .parse(renderedSql, StatementContext.create(config, null, QueryTemplate.class));
    }

    public QueryTemplateBinding<R> with(final Handle handle) {
        return new QueryTemplateBinding<>(handle, this);
    }
}
