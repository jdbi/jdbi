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
package org.jdbi.v3.core.mapper.reflect;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.jdbi.v3.core.config.JdbiConfig;

/**
 * Configuration class for reflective mappers.
 */
public class ReflectionMappers implements JdbiConfig<ReflectionMappers> {
    private List<ColumnNameMatcher> columnNameMatchers;
    private boolean strictMatching;

    /**
     * Create a default configuration that attempts case insensitive and
     * snake_case matching for names.
     */
    public ReflectionMappers() {
        columnNameMatchers = Arrays.asList(
                new CaseInsensitiveColumnNameMatcher(),
                new SnakeCaseColumnNameMatcher());
    }

    private ReflectionMappers(ReflectionMappers that) {
        columnNameMatchers = new ArrayList<>(that.columnNameMatchers);
    }

    /**
     * @return the registered column name mappers
     */
    public List<ColumnNameMatcher> getColumnNameMatchers() {
        return Collections.unmodifiableList(columnNameMatchers);
    }

    /**
     * Replace all column name matchers with the given list.
     * @param columnNameMatchers the column name matchers to use
     * @return this
     */
    public ReflectionMappers setColumnNameMatchers(List<ColumnNameMatcher> columnNameMatchers) {
        this.columnNameMatchers = new ArrayList<>(columnNameMatchers);
        return this;
    }

    /**
     * @return if strict column name matching is enabled
     */
    public boolean isStrictMatching() {
        return this.strictMatching;
    }

    /**
     * Throw an IllegalArgumentException if a the set of fields doesn't
     * match to columns exactly.
     *
     * @param strictMatching whether to enable strict matching
     * @return this
     */
    public ReflectionMappers setStrictMatching(boolean strictMatching) {
        this.strictMatching = strictMatching;
        return this;
    }

    /**
     * Determine whether any column name matcher thinks the given property and column names are the same.
     * @param columnName the column name to compare
     * @param javaName the Java name to compare
     * @return if they are the same property
     */
    public boolean columnNameMatches(String columnName, String javaName) {
        return columnNameMatchers.stream()
                .map(m -> m.columnNameMatches(columnName, javaName))
                .filter(matches -> matches)
                .findAny()
                .orElse(false);
    }

    /**
     * Determine the parameter name to be used for a bean property.
     * @param descriptor the property
     * @return the preferred name to match to columns
     */
    public String paramName(PropertyDescriptor descriptor) {
        return Stream.of(descriptor.getReadMethod(), descriptor.getWriteMethod())
                .filter(Objects::nonNull)
                .map(method -> method.getAnnotation(ColumnName.class))
                .filter(Objects::nonNull)
                .map(ColumnName::value)
                .findFirst()
                .orElseGet(descriptor::getName);
    }

    @Override
    public ReflectionMappers createCopy() {
        return new ReflectionMappers(this);
    }
}
