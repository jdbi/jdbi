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

import org.jdbi.v3.core.cache.JdbiCache;
import org.jdbi.v3.core.cache.JdbiCacheBuilder;
import org.jdbi.v3.core.cache.internal.DefaultJdbiCacheBuilder;
import org.jdbi.v3.meta.Beta;

public abstract class CachingSqlParser implements SqlParser {

    /** The default size of the parsed SQL cache. */
    public static final int PARSED_SQL_CACHE_SIZE = 1_000;

    private final JdbiCache<String, ParsedSql> parsedSqlCache;

    CachingSqlParser() {
        this(DefaultJdbiCacheBuilder.builder().maxSize(PARSED_SQL_CACHE_SIZE));
    }

    CachingSqlParser(JdbiCacheBuilder cacheBuilder) {
        parsedSqlCache = cacheBuilder.buildWithLoader(this::internalParse);
    }

    @Override
    public ParsedSql parse(String sql, StatementContext ctx) {
        try {
            return parsedSqlCache.get(sql);
        } catch (IllegalArgumentException e) {
            throw new UnableToCreateStatementException("Exception parsing for named parameter replacement", e, ctx);
        }
    }

    /**
     * Returns cache statistics for the internal sql parser cache. This returns a cache specific object,
     * so the user needs to know what caching library is in use.
     */
    @Beta
    public <T> T cacheStats() {
        return parsedSqlCache.getStats();
    }

    abstract ParsedSql internalParse(String sql);
}
