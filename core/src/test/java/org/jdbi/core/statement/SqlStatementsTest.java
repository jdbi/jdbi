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
package org.jdbi.core.statement;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Map;

import org.jdbi.core.cache.internal.DefaultJdbiCacheBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqlStatementsTest {

    // Default flag/int vector of a freshly constructed SqlStatements.
    private static void assertDefaultFlags(final SqlStatements s) {
        assertFlags(s, false, false, true, true, true, 512, 512);
    }

    // Asserts the full boolean + int field vector at once, so any positional-argument swap in the
    // all-arguments constructor (five booleans and two ints) is caught by exactly one wither test.
    private static void assertFlags(final SqlStatements s,
            final boolean unusedBindingAllowed,
            final boolean attachAll,
            final boolean attachCallback,
            final boolean scriptSemicolon,
            final boolean includeBindings,
            final int jfrSql,
            final int jfrParam) {
        assertThat(s.isUnusedBindingAllowed()).as("unusedBindingAllowed").isEqualTo(unusedBindingAllowed);
        assertThat(s.isAttachAllStatementsForCleanup()).as("attachAllStatementsForCleanup").isEqualTo(attachAll);
        assertThat(s.isAttachCallbackStatementsForCleanup()).as("attachCallbackStatementsForCleanup").isEqualTo(attachCallback);
        assertThat(s.isScriptStatementsNeedSemicolon()).as("scriptStatementsNeedSemicolon").isEqualTo(scriptSemicolon);
        assertThat(s.getIncludeBindingsInTelemetry()).as("includeBindingsInTelemetry").isEqualTo(includeBindings);
        assertThat(s.getJfrSqlMaxLength()).as("jfrSqlMaxLength").isEqualTo(jfrSql);
        assertThat(s.getJfrParamMaxLength()).as("jfrParamMaxLength").isEqualTo(jfrParam);
    }

    @Test
    void defaults() {
        final SqlStatements s = new SqlStatements();
        assertDefaultFlags(s);
        assertThat(s.getQueryTimeout()).isNull();
        assertThat(s.getSqlLogger()).isSameAs(SqlLogger.NOP_SQL_LOGGER);
        assertThat(s.getAttributes()).isEmpty();
        assertThat(s.getCustomizers()).isEmpty();
        assertThat(s.getContextListeners()).isEmpty();
    }

    // --- Each boolean/int wither flips exactly one field (guards the all-args constructor order). ---

    @Test
    void unusedBindingAllowedFlipsOnlyItsField() {
        assertFlags(new SqlStatements().unusedBindingAllowed(true), true, false, true, true, true, 512, 512);
    }

    @Test
    void attachAllStatementsForCleanupFlipsOnlyItsField() {
        assertFlags(new SqlStatements().attachAllStatementsForCleanup(true), false, true, true, true, true, 512, 512);
    }

    @Test
    void attachCallbackStatementsForCleanupFlipsOnlyItsField() {
        assertFlags(new SqlStatements().attachCallbackStatementsForCleanup(false), false, false, false, true, true, 512, 512);
    }

    @Test
    void scriptStatementsNeedSemicolonFlipsOnlyItsField() {
        assertFlags(new SqlStatements().scriptStatementsNeedSemicolon(false), false, false, true, false, true, 512, 512);
    }

    @Test
    void includeBindingsInTelemetryFlipsOnlyItsField() {
        assertFlags(new SqlStatements().includeBindingsInTelemetry(false), false, false, true, true, false, 512, 512);
    }

    @Test
    void jfrSqlMaxLengthSetsOnlyItsField() {
        assertFlags(new SqlStatements().jfrSqlMaxLength(10), false, false, true, true, true, 10, 512);
    }

    @Test
    void jfrParamMaxLengthSetsOnlyItsField() {
        assertFlags(new SqlStatements().jfrParamMaxLength(20), false, false, true, true, true, 512, 20);
    }

    // --- Non-flag withers set their field and preserve the flag vector (also guards the constructor). ---

    @Test
    void templateEngineWither() {
        final SqlStatements base = new SqlStatements();
        final SqlStatements derived = base.templateEngine(TemplateEngine.NOP);

        assertThat(derived.getTemplateEngine()).isSameAs(TemplateEngine.NOP);
        assertThat(derived).isNotSameAs(base);
        assertDefaultFlags(derived);
        assertThat(base.getTemplateEngine()).isNotSameAs(TemplateEngine.NOP);
    }

    @Test
    void sqlParserWither() {
        final SqlStatements base = new SqlStatements();
        final SqlParser parser = new HashPrefixSqlParser();
        final SqlStatements derived = base.sqlParser(parser);

        assertThat(derived.getSqlParser()).isSameAs(parser);
        assertThat(derived).isNotSameAs(base);
        assertDefaultFlags(derived);
        assertThat(base.getSqlParser()).isNotSameAs(parser);
    }

    @Test
    void sqlLoggerWither() {
        final SqlStatements base = new SqlStatements();
        final SqlLogger logger = new SqlLogger() {};
        final SqlStatements derived = base.sqlLogger(logger);

        assertThat(derived.getSqlLogger()).isSameAs(logger);
        assertDefaultFlags(derived);
        assertThat(base.getSqlLogger()).isSameAs(SqlLogger.NOP_SQL_LOGGER);
    }

    @Test
    void sqlLoggerNullResetsToNop() {
        assertThat(new SqlStatements().sqlLogger(null).getSqlLogger()).isSameAs(SqlLogger.NOP_SQL_LOGGER);
    }

    @Test
    void queryTimeoutWither() {
        final SqlStatements base = new SqlStatements();
        final SqlStatements derived = base.queryTimeout(42);

        assertThat(derived.getQueryTimeout()).isEqualTo(42);
        assertDefaultFlags(derived);
        assertThat(base.getQueryTimeout()).isNull();
    }

    @Test
    void queryTimeoutRejectsNegative() {
        assertThatThrownBy(() -> new SqlStatements().queryTimeout(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Attributes ---

    @Test
    void defineDoesNotMutateReceiver() {
        final SqlStatements base = new SqlStatements();
        final SqlStatements derived = base.define("x", "y");

        assertThat(base.getAttribute("x")).isNull();
        assertThat(derived.getAttribute("x")).isEqualTo("y");
        assertThat(derived.getAttributes()).containsEntry("x", "y");
    }

    @Test
    void defineToleratesNullValue() {
        final SqlStatements derived = new SqlStatements().define("x", null);
        assertThat(derived.getAttribute("x")).isNull();
        assertThat(derived.getAttributes()).containsKey("x");
    }

    @Test
    void defineMapMerges() {
        final SqlStatements derived = new SqlStatements().define("a", 1).defineMap(Map.of("b", 2, "c", 3));
        assertThat(derived.getAttributes()).containsEntry("a", 1).containsEntry("b", 2).containsEntry("c", 3);
    }

    @Test
    void defineMapNullOrEmptyIsNoOp() {
        final SqlStatements base = new SqlStatements().define("a", 1);
        assertThat(base.defineMap(null)).isSameAs(base);
        assertThat(base.defineMap(Map.of())).isSameAs(base);
    }

    @Test
    void getAttributesReturnsIndependentCopy() {
        final SqlStatements s = new SqlStatements().define("a", 1);
        final Map<String, Object> attrs = s.getAttributes();
        attrs.put("b", 2);
        assertThat(s.getAttributes()).doesNotContainKey("b");
    }

    // --- Registration collections ---

    @Test
    void addCustomizerAppendsInRegistrationOrder() {
        final StatementCustomizer c1 = new StatementCustomizer() {};
        final StatementCustomizer c2 = new StatementCustomizer() {};
        final SqlStatements base = new SqlStatements();
        final SqlStatements derived = base.addCustomizer(c1).addCustomizer(c2);

        assertThat(base.getCustomizers()).isEmpty();
        assertThat(derived.getCustomizers()).containsExactly(c1, c2);
    }

    @Test
    void addContextListenerDedupsAndKeepsInsertionOrder() {
        final StatementContextListener l1 = new StatementContextListener() {};
        final StatementContextListener l2 = new StatementContextListener() {};
        final SqlStatements derived = new SqlStatements()
                .addContextListener(l1)
                .addContextListener(l2)
                .addContextListener(l1);

        assertThat(derived.getContextListeners()).containsExactly(l1, l2);
    }

    @Test
    void exceptionHandlersFireLatestRegisteredFirst() {
        final RuntimeException first = new RuntimeException("first");
        final RuntimeException second = new RuntimeException("second");
        final SqlStatements s = new SqlStatements()
                .addExceptionHandler(e -> first)
                .addExceptionHandler(e -> second);

        // ctx is only consulted when no handler rewrites the exception, so null is fine here.
        assertThatThrownBy(() -> {
            throw s.handleException(new SQLException("boom"), null);
        }).isSameAs(second);
    }

    @Test
    void exceptionHandlerReturningNullFallsThrough() {
        final RuntimeException rewritten = new RuntimeException("rewritten");
        final SqlStatements s = new SqlStatements()
                .addExceptionHandler(e -> rewritten)
                .addExceptionHandler(e -> null);

        assertThatThrownBy(() -> {
            throw s.handleException(new SQLException("boom"), null);
        }).isSameAs(rewritten);
    }

    // --- Template cache: shared across ordinary derivations, fresh only via templateCache(). ---

    @Test
    void ordinaryWithersShareTemplateCache() throws Exception {
        final SqlStatements base = new SqlStatements();
        final SqlStatements derived = base.unusedBindingAllowed(true).define("a", 1);

        assertThat(templateCache(derived)).isSameAs(templateCache(base));
    }

    @Test
    void templateCacheWitherInstallsFreshCache() throws Exception {
        final SqlStatements base = new SqlStatements();
        final SqlStatements derived = base.templateCache(DefaultJdbiCacheBuilder.builder().maxSize(10));

        assertThat(derived).isNotSameAs(base);
        assertThat(templateCache(derived)).isNotSameAs(templateCache(base));
        assertDefaultFlags(derived);
    }

    @Test
    void createCopyReturnsSameImmutableInstance() {
        final SqlStatements s = new SqlStatements().define("a", 1);
        assertThat(s.createCopy()).isSameAs(s);
    }

    private static Object templateCache(final SqlStatements s) throws Exception {
        final Field field = SqlStatements.class.getDeclaredField("templateCache");
        field.setAccessible(true);
        return field.get(s);
    }
}
