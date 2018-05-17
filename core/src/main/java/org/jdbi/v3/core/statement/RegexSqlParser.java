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

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexSqlParser implements SqlParser {
    private final Pattern pattern;
    private final String prefix, suffix;

    /**
     * @param prefix the <b>literal</b> prefix to match, e.g. <code>"${"</code>
     * @param suffix the <b>literal</b> suffix to match, e.g. <code>"}"</code>
     */
    public RegexSqlParser(String prefix, String suffix) {
        this.pattern = Pattern.compile(Pattern.quote(prefix) + "([a-z0-9_]+)" + Pattern.quote(suffix));
        this.prefix = prefix;
        this.suffix = suffix;
    }

    @Override
    public ParsedSql parse(String sql, StatementContext ctx) {
        ParsedSql.Builder builder = ParsedSql.builder();
        Matcher matcher = pattern.matcher(sql);

        int lastIndex = 0;
        while (matcher.find()) {
            MatchResult r = matcher.toMatchResult();
            builder.append(sql.substring(lastIndex, r.start()));
            builder.appendNamedParameter(r.group(1));
            lastIndex = r.end();
        }
        builder.append(sql.substring(lastIndex));

        return builder.build();
    }

    @Override
    public String nameParameter(String rawName, StatementContext ctx) {
        return prefix + rawName + suffix;
    }
}
