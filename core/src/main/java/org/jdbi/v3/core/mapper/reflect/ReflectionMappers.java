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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.mapper.CaseStrategy;
import org.jdbi.v3.meta.Beta;

/**
 * Configuration class for reflective mappers.
 */
public class ReflectionMappers implements JdbiConfig<ReflectionMappers> {
    private List<ColumnNameMatcher> columnNameMatchers;
    private boolean strictMatching;
    private UnaryOperator<String> caseChange;

    /**
     * Create a default configuration that attempts case insensitive and
     * snake_case matching for names.
     */
    public ReflectionMappers() {
        columnNameMatchers = Arrays.asList(
                new CaseInsensitiveColumnNameMatcher(),
                new SnakeCaseColumnNameMatcher());
        strictMatching = false;
        caseChange = CaseStrategy.LOCALE_LOWER;
    }

    private ReflectionMappers(ReflectionMappers that) {
        columnNameMatchers = new ArrayList<>(that.columnNameMatchers);
        strictMatching = that.strictMatching;
        caseChange = that.caseChange;
    }

    /**
     * Returns the registered column name mappers.
     *
     * @return the registered column name mappers.
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
     * Returns whether strict column name matching is enabled.
     *
     * @return True if strict column name matching is enabled.
     */
    public boolean isStrictMatching() {
        return this.strictMatching;
    }

    /**
     * Throw an IllegalArgumentException if a the set of fields doesn't
     * match to columns exactly.
     *
     * Reflection mappers with prefixes will only check those columns that
     * begin with the mapper's prefix.
     *
     * @param strictMatching whether to enable strict matching
     * @return this
     */
    public ReflectionMappers setStrictMatching(boolean strictMatching) {
        this.strictMatching = strictMatching;
        return this;
    }

    /**
     * Case change strategy for the database column names. By default, the row names are lowercased using the system locale.
     *
     * @return The current case change strategy.
     * @see CaseStrategy
     */
    @Beta
    public UnaryOperator<String> getCaseChange() {
        return caseChange;
    }

    /**
     * Sets the case change strategy for the database column names. By default, the row names are lowercased using the system locale.
     *
     * @param caseChange The strategy to use. Must not be null.
     * @see CaseStrategy
     */
    @Beta
    public ReflectionMappers setCaseChange(UnaryOperator<String> caseChange) {
        this.caseChange = caseChange;
        return this;
    }

    @Override
    public ReflectionMappers createCopy() {
        return new ReflectionMappers(this);
    }
}
