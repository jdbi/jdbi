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
package org.jdbi.cache.caffeine;

import org.jdbi.core.Jdbi;
import org.jdbi.core.spi.JdbiPlugin;
import org.jdbi.core.statement.ColonPrefixSqlParser;
import org.jdbi.core.statement.SqlStatements;

import static org.jdbi.core.statement.CachingSqlParser.PARSED_SQL_CACHE_SIZE;
import static org.jdbi.core.statement.SqlStatements.SQL_TEMPLATE_CACHE_SIZE;

/**
 * Installing this plugin restores the up-to 3.36.0 behavior of using the Caffeine cache library for SQL statements and the colon prefix parser.
 */
public final class CaffeineCachePlugin implements JdbiPlugin {

    @Override
    public void customizeJdbi(Jdbi jdbi) {
        final SqlStatements config = jdbi.getConfig(SqlStatements.class);

        config.setTemplateCache(CaffeineCacheBuilder.instance().maxSize(SQL_TEMPLATE_CACHE_SIZE));
        config.setSqlParser(new ColonPrefixSqlParser(CaffeineCacheBuilder.instance().maxSize(PARSED_SQL_CACHE_SIZE)));
    }
}
