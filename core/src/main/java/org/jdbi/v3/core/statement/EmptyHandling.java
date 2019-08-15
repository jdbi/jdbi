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

import java.util.function.BiConsumer;

/**
 * describes what should be done if the value passed to {@link SqlStatement#bindList} is null or empty
 */
public enum EmptyHandling implements BiConsumer<SqlStatement, String> {
    /**
     * <p>Render nothing in the query.</p>
     * <p>
     * {@code select * from things where x in ()}
     */
    BLANK {
        @Override
        public void accept(SqlStatement stmt, String key) {
            stmt.define(key, "");
        }
    },
    /**
     * <p>Render the keyword {@code null} in the query, useful e.g. in postgresql where "in ()" is invalid syntax.</p>
     * <p>
     * {@code select * from things where x in (null)}
     */
    NULL_KEYWORD {
        @Override
        public void accept(SqlStatement stmt, String key) {
            stmt.define(key, "null");
        }
    },
    /**
     * <p>Define a {@code null} value, leaving the result up to the {@link org.jdbi.v3.core.statement.TemplateEngine} to decide.</p>
     * <p>
     * This value was specifically added to <a href="https://github.com/jdbi/jdbi/issues/1377">make conditionals work better with <code>StringTemplate</code></a>.
     */
    DEFINE_NULL {
        @Override
        public void accept(SqlStatement stmt, String key) {
            stmt.define(key, null);
        }
    },
    /**
     * Throw IllegalArgumentException.
     */
    THROW {
        @Override
        public void accept(SqlStatement stmt, String key) {
            throw new IllegalArgumentException("argument is null or empty; this is forbidden on this call to `bindList`");
        }
    }
}
