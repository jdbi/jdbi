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
package org.jdbi.v3.core.argument;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

import org.jdbi.v3.core.statement.StatementContext;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

/**
 * Check that {@link java.time.LocalDate} arguments are bound to corresponding
 * {@link java.sql.Date} values.
 */
public class TestLocalDateArgument {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    PreparedStatement stmt;

    @Mock
    StatementContext ctx;

    @Test
    public void testBindLocalDate() throws SQLException {
        ArgumentFactory factory = BuiltInArgumentFactory.INSTANCE;

        LocalDate date = LocalDate.of(2001, 1, 1);

        Optional<Argument> optionalArgument = factory.build(LocalDate.class, date, null);
        assertTrue(optionalArgument.isPresent());

        Argument argument = optionalArgument.get();
        argument.apply(5, stmt, ctx);

        verify(stmt).setDate(5, java.sql.Date.valueOf(date));
    }

}
