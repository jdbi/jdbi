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
package org.jdbi.core.mapper.reflect;

import java.lang.reflect.AccessibleObject;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.jdbi.core.config.JdbiConfig;
import org.jdbi.core.mapper.CaseStrategy;
import org.jdbi.meta.Alpha;

import static org.jdbi.core.mapper.reflect.AccessibleObjectStrategy.DO_NOT_MAKE_ACCESSIBLE;
import static org.jdbi.core.mapper.reflect.AccessibleObjectStrategy.FORCE_MAKE_ACCESSIBLE;

/**
 * Configuration class for reflective mappers.
 */
public class ReflectionMappers implements JdbiConfig<ReflectionMappers> {

    private final List<ColumnNameMatcher> columnNameMatchers;
    private final boolean strictMatching;
    private final UnaryOperator<String> caseChange;
    private final Consumer<AccessibleObject> makeAccessible;

    /**
     * Create a default configuration that attempts case insensitive and
     * snake_case matching for names.
     */
    public ReflectionMappers() {
        this(List.of(
                new CaseInsensitiveColumnNameMatcher(),
                new SnakeCaseColumnNameMatcher()),
            false,
            CaseStrategy.LOCALE_LOWER,
            FORCE_MAKE_ACCESSIBLE);
    }

    private ReflectionMappers(List<ColumnNameMatcher> columnNameMatchers,
            boolean strictMatching,
            UnaryOperator<String> caseChange,
            Consumer<AccessibleObject> makeAccessible) {
        this.columnNameMatchers = List.copyOf(columnNameMatchers);
        this.strictMatching = strictMatching;
        this.caseChange = caseChange;
        this.makeAccessible = makeAccessible;
    }

    /**
     * Returns the registered column name mappers.
     *
     * @return the registered column name mappers.
     */
    public List<ColumnNameMatcher> getColumnNameMatchers() {
        return columnNameMatchers;
    }

    /**
     * Returns a copy of this configuration with all column name matchers replaced by the given list.
     * @param columnNameMatchers the column name matchers to use
     * @return the derived configuration
     */
    public ReflectionMappers columnNameMatchers(List<ColumnNameMatcher> columnNameMatchers) {
        return new ReflectionMappers(columnNameMatchers, strictMatching, caseChange, makeAccessible);
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
     * Returns a copy of this configuration that throws an IllegalArgumentException if the set of fields doesn't
     * match to columns exactly.
     *
     * Reflection mappers with prefixes will only check those columns that
     * begin with the mapper's prefix.
     *
     * @param strictMatching whether to enable strict matching
     * @return the derived configuration
     */
    public ReflectionMappers strictMatching(boolean strictMatching) {
        return new ReflectionMappers(columnNameMatchers, strictMatching, caseChange, makeAccessible);
    }

    /**
     * Case change strategy for the database column names. By default, the row names are lowercased using the system locale.
     *
     * @return The current case change strategy.
     * @see CaseStrategy
     */
    public UnaryOperator<String> getCaseChange() {
        return caseChange;
    }

    /**
     * Returns a copy of this configuration with the given case change strategy for the database column names.
     * By default, the row names are lowercased using the system locale.
     *
     * @param caseChange The strategy to use. Must not be null.
     * @return the derived configuration
     * @see CaseStrategy
     */
    public ReflectionMappers caseChange(UnaryOperator<String> caseChange) {
        return new ReflectionMappers(columnNameMatchers, strictMatching, caseChange, makeAccessible);
    }

    /**
     * Returns a copy of this configuration with the given strategy for Java accessibility rules.
     * The legacy default is to call {@code setAccessible(true)} in certain cases when we try to use a Constructor, Method, or Field.
     * In the future, this default will be changed to a no-op, to better interact with the Java module system.
     *
     * @param makeAccessible A {@link Consumer} instance that implements the strategy.
     * @return the derived configuration
     * @see AccessibleObjectStrategy
     *
     */
    @Alpha
    public ReflectionMappers accessibleObjectStrategy(Consumer<AccessibleObject> makeAccessible) {
        return new ReflectionMappers(columnNameMatchers, strictMatching, caseChange, makeAccessible);
    }

    /**
     * Returns a copy of this configuration with the strategy Jdbi uses for Java accessibility rules set to a no-op.
     *
     * @return the derived configuration
     */
    @Alpha
    public ReflectionMappers disableAccessibleObjectStrategy() {
        return accessibleObjectStrategy(DO_NOT_MAKE_ACCESSIBLE);
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
        // Immutable: safe to share across registries.
        return this;
    }
}
