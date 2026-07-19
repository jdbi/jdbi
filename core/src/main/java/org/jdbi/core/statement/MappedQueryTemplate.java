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
import org.jdbi.core.mapper.RowMapper;

/**
 * A {@link QueryTemplate} whose result type is fixed: the {@link RowMapper} is resolved once, at build
 * time, from the template's configuration snapshot. Because the template already holds its mapper, each
 * execution skips the per-call mapper lookup that {@code mapTo(type)} otherwise repeats.
 *
 * <p>Obtain one from {@link QueryTemplate#mapTo(Class)} (or a {@code GenericType} / {@code QualifiedType}
 * overload), then execute it by binding to a {@link Handle} with {@link #with(Handle)}, exactly as with a
 * plain {@link QueryTemplate}:
 *
 * <pre>{@code
 * MappedQueryTemplate<String> byId =
 *     jdbi.buildQueryTemplate("SELECT name FROM users WHERE id = :id").mapTo(String.class);
 * String name = byId.with(handle).bind("id", id).results().one();
 * }</pre>
 *
 * @param <T> the result type this template maps rows to
 */
public class MappedQueryTemplate<T> {
    private final QueryTemplate template;
    private final RowMapper<T> mapper;

    MappedQueryTemplate(final QueryTemplate template, final RowMapper<T> mapper) {
        this.template = template;
        this.mapper = mapper;
    }

    /**
     * Binds this template to a handle for a single execution. The returned binding is thread-confined;
     * obtain a fresh one for each execution.
     *
     * @param handle the handle to execute against
     * @return a fresh, thread-confined binding
     */
    public BoundMappedQuery<T> with(final Handle handle) {
        return new BoundMappedQuery<>(template.with(handle), mapper);
    }
}
