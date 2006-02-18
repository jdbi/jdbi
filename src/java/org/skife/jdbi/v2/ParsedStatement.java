/* Copyright 2004-2006 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.v2;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.ArrayList;

class ParsedStatement
{
    private static final Pattern TOKEN_PATTERN = Pattern.compile(":(\\w+)");
    private static final Pattern QUOTE_PATTERN = Pattern.compile("'.*'");

//    private static final Pattern IN_PARAM_PATTERN = Pattern.compile("\\sin\\s*\\(\\?\\)");

    private static final String[] EMPTY = new String[]{};

    private final String[] tokens;
    private final String replaced;
    private boolean positionalOnly;

    public String[] getNamedParams()
    {
        return tokens;
    }

    public String getSubstitutedSql()
    {
        return replaced;
    }

//    boolean isDynamic()
//    {
//        return IN_PARAM_PATTERN.matcher(replaced).find();
//    }

    ParsedStatement(final String sql)
    {
        // attempt to short circuit
        if (sql.indexOf(":") == -1)
        {
            positionalOnly = true;
            // no named params, short circuit
            this.tokens = EMPTY;
            this.replaced = sql;
            return;
        }

        final Matcher token_matcher = TOKEN_PATTERN.matcher(sql);
        final Matcher quote_matcher = QUOTE_PATTERN.matcher(sql);
        boolean last_quote;
        boolean last_token;

        if (!(last_quote = quote_matcher.find()))
        {
            // we have no quotes, just replace tokens if there are any and exit
            this.replaced = token_matcher.replaceAll("?");
            token_matcher.reset();
            final List<String> tokens = new ArrayList<String>();
            while (token_matcher.find())
            {
                tokens.add(token_matcher.group().substring(token_matcher.group().indexOf(":") + 1));
            }
            this.tokens = tokens.toArray(new String[tokens.size()]);
        }
        else if (last_token = token_matcher.find())
        {
            // we have quotes and tokens, juggling time
            final List<String> tokens = new ArrayList<String>();
            final StringBuffer replaced = new StringBuffer();

            // copy everything up to beginning of first match into buffer
            final int end = quote_matcher.start() > token_matcher.start()
                            ? token_matcher.start()
                            : quote_matcher.start();
            replaced.append(sql.substring(0, end));

            do
            {
                if (last_token && last_quote)
                {
                    // token and quote, crappo
                    if (token_matcher.end() < quote_matcher.start())
                    {
                        // token is before the quote
                        // append ? and stuff to start of quote
                        replaced.append("?");
                        tokens.add(token_matcher.group().substring(1, token_matcher.group().length()));
                        replaced.append(sql.substring(token_matcher.end(), quote_matcher.start()));
                        last_token = token_matcher.find();
                    }
                    else if (token_matcher.start() > quote_matcher.end())
                    {
                        // token is after quote
                        replaced.append(sql.substring(quote_matcher.start(), token_matcher.start()));
                        last_quote = quote_matcher.find();
                    }
                    else
                    {
                        // token is inside quote
                        replaced.append(sql.substring(quote_matcher.start(), quote_matcher.end()));

                        // iterate through tokens until we escape the quote
                        while (last_token = token_matcher.find())
                        {
                            if (token_matcher.start() > quote_matcher.end())
                            {
                                // found a token after the quote
                                break;
                            }
                        } // or iterated through string and no more tokens

                        last_quote = quote_matcher.find();
                    }
                }
                else if (last_token)
                {
                    // found a token, but no more quotes

                    replaced.append("?");
                    tokens.add(token_matcher.group().substring(1, token_matcher.group().length()));
                    int index = token_matcher.end();
                    last_token = token_matcher.find();
                    replaced.append(sql.substring(index, (last_token ? token_matcher.start() : sql.length())));
                }
                else if (last_quote)
                {
                    // quote, but no more tokens
                    replaced.append(sql.substring(quote_matcher.start(), sql.length()));
                    last_quote = false;
                }
            }
            while (last_token || last_quote);

            this.replaced = replaced.toString();
            this.tokens = tokens.toArray(new String[tokens.size()]);
        }
        else
        {
            // no quotes, no tokens, piece of cake
            tokens = EMPTY;
            replaced = sql;
        }
    }

    public boolean isPositionalOnly()
    {
        return positionalOnly;
    }
}
