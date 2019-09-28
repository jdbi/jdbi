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

import java.sql.PreparedStatement;
import java.util.Arrays;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.qualifier.Qualifiers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class ArgumentBinderTest {
    private static final Argument NOP = (position, statement, ctx) -> {};

    @Rule
    public MethodRule mockito = MockitoJUnit.rule();
    @Mock
    private PreparedStatement stmt;
    @Mock
    private StatementContext ctx;

    private SqlStatements statements = new SqlStatements();

    @Before
    public void before() {
        when(ctx.getConfig(Qualifiers.class)).thenReturn(new Qualifiers());
        when(ctx.getConfig(SqlStatements.class)).thenReturn(statements);
    }

    @Test
    public void testPositionalEmpty() {
        assertThatCode(() -> new ArgumentBinder<>(stmt, ctx, positionalParams(0)).bind(positionalBinding(0)))
            .doesNotThrowAnyException();
    }

    @Test
    public void testNamedEmpty() {
        assertThatCode(() -> new ArgumentBinder<>(stmt, ctx, namedParams()).bind(namedBinding()))
            .doesNotThrowAnyException();
    }

    @Test
    public void testPositionalDeclaredButNotProvided() {
        assertThatThrownBy(() -> new ArgumentBinder<>(stmt, ctx, positionalParams(1)).bind(positionalBinding(0)))
            .isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    public void testNamedDeclaredButNotProvided() {
        assertThatThrownBy(() -> new ArgumentBinder<>(stmt, ctx, namedParams("unused")).bind(namedBinding()))
            .isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    public void testPositionalNotDeclaredButProvided() {
        assertThatThrownBy(() -> new ArgumentBinder<>(stmt, ctx, positionalParams(0)).bind(positionalBinding(1)))
            .isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    public void testNamedNotDeclaredButProvided() {
        assertThatThrownBy(() -> new ArgumentBinder<>(stmt, ctx, namedParams()).bind(namedBinding("unused")))
            .isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    public void testPositionalNotDeclaredButProvidedWithPermission() {
        allowUnused();

        assertThatCode(() -> new ArgumentBinder<>(stmt, ctx, positionalParams(0)).bind(positionalBinding(1)))
            .doesNotThrowAnyException();
    }

    @Test
    public void testNamedNotDeclaredButProvidedWithPermission() {
        allowUnused();

        assertThatCode(() -> new ArgumentBinder<>(stmt, ctx, namedParams()).bind(namedBinding("unused")))
            .doesNotThrowAnyException();
    }

    private ParsedParameters positionalParams(int size) {
        String[] names = new String[size];
        Arrays.fill(names, "?");
        return new ParsedParameters(true, Arrays.asList(names));
    }

    private ParsedParameters namedParams(String... names) {
        return new ParsedParameters(false, Arrays.asList(names));
    }

    private Binding positionalBinding(int size) {
        Binding b = new Binding(ctx);
        for (int i = 0; i < size; i++) {
            b.addPositional(i, NOP);
        }
        return b;
    }

    private Binding namedBinding(String... names) {
        Binding b = new Binding(ctx);
        for (String name: names) {
            b.addNamed(name, NOP);
        }
        return b;
    }

    private void allowUnused() {
        statements.setUnusedBindingAllowed(true);
    }
}
