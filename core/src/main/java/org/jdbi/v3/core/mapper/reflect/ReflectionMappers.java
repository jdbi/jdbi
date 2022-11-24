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

import java.lang.reflect.AccessibleObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.mapper.CaseStrategy;
import org.jdbi.v3.meta.Alpha;
import org.jdbi.v3.meta.Beta;

/**
 * Configuration class for reflective mappers.
 */
@SuppressWarnings("HiddenField")
public class ReflectionMappers implements JdbiConfig<ReflectionMappers> {

    private static final Consumer<AccessibleObject> FORCE_MAKE_ACCESSIBLE = accessibleObject -> accessibleObject.setAccessible(true);
    private static final Consumer<AccessibleObject> DO_NOT_MAKE_ACCESSIBLE = accessibleObject -> {};

    private List<ColumnNameMatcher> columnNameMatchers;
    private boolean strictMatching;
    private UnaryOperator<String> caseChange;
    private Consumer<AccessibleObject> makeAccessible;

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
        makeAccessible = FORCE_MAKE_ACCESSIBLE;
    }

    private ReflectionMappers(ReflectionMappers that) {
        columnNameMatchers = new ArrayList<>(that.columnNameMatchers);
        strictMatching = that.strictMatching;
        caseChange = that.caseChange;
        makeAccessible = that.makeAccessible;
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

    /**
     * Set the strategy Jdbi uses for Java accessibility rules.
     * The legacy default is to call {@code setAccessible(true)} in certain cases when we try to use a Constructor, Method, or Field.
     * In the future, this default will be changed to a no-op, to better interact with the Java module system.
     */
    @Alpha
    public ReflectionMappers setAccessibleObjectStrategy(Consumer<AccessibleObject> makeAccessible) {
        this.makeAccessible = makeAccessible;
        return this;
    }

    /**
     * Set the strategy Jdbi uses for Java accessibility rules to a no-op.
     */
    @Alpha
    public ReflectionMappers disableAccessibleObjectStrategy() {
        this.makeAccessible = DO_NOT_MAKE_ACCESSIBLE;
        return this;
    }

    /**
     * Use the accessibility strategy to potentially make a reflective operation accessible.
     */
    @Alpha
    public <T extends AccessibleObject> T makeAccessible(T accessibleObject) {
        makeAccessible.accept(accessibleObject);
        return accessibleObject;
    }

    @Override
    public ReflectionMappers createCopy() {
        return new ReflectionMappers(this);
    }
}
