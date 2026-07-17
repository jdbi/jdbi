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
import org.jdbi.core.result.ResultBearing;

/**
 * A reusable, immutable, thread-safe query definition. Built once from a {@link org.jdbi.core.Jdbi}
 * (see {@code Jdbi.buildQueryTemplate}) and executed many times by binding it to a {@link Handle}
 * with {@link #with(Handle)}.
 *
 * <p>The SQL is rendered and parsed a single time when the template is built; the resulting
 * {@link ParsedSql} and the configuration are shared read-only. Each execution binds its own
 * parameters and applies a result operation through the {@link ResultBearing} methods on the
 * {@link QueryTemplateBinding} returned by {@link #with(Handle)}, exactly as with a
 * {@link Query}.
 */
public class QueryTemplate {
    final ConfigRegistry config;
    final String sql;

    // Rendered and parsed once at build time, then reused for every execution. Both are null when the
    // SQL depends on attributes supplied per execution (so it cannot be rendered once here); each
    // execution then renders and parses with its own defined attributes.
    final String renderedSql;
    final ParsedSql parsedSql;

    /**
     * Builds a template over the given SQL and configuration snapshot. The configuration is used
     * read-only and is not copied; callers pass a snapshot the template may retain (for example,
     * {@code jdbi.getConfig().createCopy()}).
     *
     * @param config the configuration snapshot to render, parse, and execute against
     * @param sql    the SQL to render and parse once
     */
    public QueryTemplate(final ConfigRegistry config, final CharSequence sql) {
        this.config = config;
        this.sql = sql.toString();

        final SqlStatements stmtConfig = config.get(SqlStatements.class);
        String rendered;
        ParsedSql parsed;
        try {
            rendered = stmtConfig.preparedRender(this.sql, RenderContext.of(config));
            // The parser uses the context only for exception reporting; parsing depends solely on the SQL.
            parsed = stmtConfig.getSqlParser()
                .parse(rendered, StatementContext.create(config, null, QueryTemplate.class));
        } catch (final RuntimeException ignored) {
            // The SQL references attributes that are only defined per execution, so it cannot be
            // rendered once here; each execution renders and parses with its own defined attributes.
            rendered = null;
            parsed = null;
        }
        this.renderedSql = rendered;
        this.parsedSql = parsed;
    }

    /**
     * Binds this template to a handle for a single execution. The returned binding is thread-confined;
     * obtain a fresh one for each execution.
     *
     * @param handle the handle to execute against
     * @return a fresh, thread-confined binding
     */
    public QueryTemplateBinding with(final Handle handle) {
        return new QueryTemplateBinding(handle, this);
    }
}
