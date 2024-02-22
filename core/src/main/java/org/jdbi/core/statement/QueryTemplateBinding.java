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

import org.jdbi.core.Handle;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.result.ResultIterable;
import org.jdbi.core.result.ResultSetScanner;
import org.jdbi.core.result.UnableToProduceResultException;

public class QueryTemplateBinding<R> implements BindingsMixin<QueryTemplateBinding<R>> {
    private final StatementContext ctx;
    private final Binding binding;
    private final String sql;
    private final ConfigRegistry config;
    private final ResultSetScanner<ResultIterable<R>> scanner;

    QueryTemplateBinding(final Handle handle, final QueryTemplate<R> template) {
        sql = template.builder.getSql();
        config = template.builder.getConfig();
        scanner = template.scanner;
        ctx = StatementContext.create(config, handle.getExtensionMethod(), getClass())
            .setConnection(handle.getConnection())
            .setRawSql(this.sql);
        binding = new Binding(ctx);
    }

    @Override
    public Binding getBinding() {
        return binding;
    }

    @Override
    @SafeVarargs
    public final <T> QueryTemplateBinding<R> bindArray(final int pos, final T... array) {
        return BindingsMixin.super.bindArray(pos, array);
    }

    @Override
    @SafeVarargs
    public final <T> QueryTemplateBinding<R> bindArray(final String name, final T... array) {
        return BindingsMixin.super.bindArray(name, array);
    }

    public ResultIterable<R> execute() {
        new Query(null, sql);
        try {
            return scanner.scanResultSet(null, ctx);
        } catch (final SQLException e) {
            throw new UnableToProduceResultException(e, ctx);
        }
    }
}
