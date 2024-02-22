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
import org.jdbi.core.result.ResultIterable;
import org.jdbi.core.result.ResultSetScanner;

public class QueryTemplate<R> {
    final QueryTemplateBuilder builder;
    final ResultSetScanner<ResultIterable<R>> scanner;

    QueryTemplate(final QueryTemplateBuilder builder, final ResultSetScanner<ResultIterable<R>> scanner) {
        this.builder = builder;
        this.scanner = scanner;
    }

    public QueryTemplateBinding<R> with(final Handle handle) {
        return new QueryTemplateBinding<>(handle, this);
    }
}
