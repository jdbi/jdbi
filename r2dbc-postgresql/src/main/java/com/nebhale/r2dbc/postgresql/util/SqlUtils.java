/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nebhale.r2dbc.postgresql.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for working with SQL.
 */
public final class SqlUtils {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\?");

    private SqlUtils() {
    }

    /**
     * Replace {@code ?} placeholders with incrementing counter place holders ({@code $1}, {@code $2}, {@code $3}, etc.).
     *
     * @param sql the sql to replace placeholders in
     * @return sql with placeholders replaced
     */
    public static String replacePlaceholders(String sql) {
        StringBuffer sb = new StringBuffer();

        int counter = 1;

        Matcher matcher = PLACEHOLDER.matcher(sql);
        while (matcher.find()) {
            matcher.appendReplacement(sb, String.format("\\$%d", counter++));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

}
