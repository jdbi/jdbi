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
package org.jdbi.v3.core.util.bean;

import java.util.Locale;

public class SnakeCaseColumnNameStrategy implements ColumnNameMappingStrategy {
    public static final SnakeCaseColumnNameStrategy INSTANCE = new SnakeCaseColumnNameStrategy();

    private final Locale locale;

    public SnakeCaseColumnNameStrategy() {
        this(Locale.ROOT);
    }

    public SnakeCaseColumnNameStrategy(Locale locale) {
        this.locale = locale;
    }

    @Override
    public boolean nameMatches(String propertyName, String sqlColumnName) {
        // Convert the property name from camel-case to underscores syntax. Freely adapted from Spring
        // BeanPropertyRowMapper.
        StringBuilder propertyNameWithUnderscores = new StringBuilder();
        propertyNameWithUnderscores.append(propertyName.substring(0, 1));
        for (int i = 1; i < propertyName.length(); i++) {
            // Do case comparison using strings rather than chars (avoid to deal with non-BMP char handling).
            String s = propertyName.substring(i, i + 1);
            String slc = s.toLowerCase(locale);
            if (!s.equals(slc)) {
                // Different cases: tokenize.
                propertyNameWithUnderscores.append("_").append(slc);
            }
            else {
                propertyNameWithUnderscores.append(s);
            }
        }
        return propertyNameWithUnderscores.toString().equals(sqlColumnName.toLowerCase(locale));
    }

    @Override
    public String toString() {
        return "SnakeCaseColumnNamingStrategy" + (locale != Locale.ROOT ? " (" + locale + ")" : "");
    }
}
