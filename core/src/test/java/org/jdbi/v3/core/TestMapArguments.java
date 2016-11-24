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
package org.jdbi.v3.core;

import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.Map;

import org.jdbi.v3.core.argument.Argument;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class TestMapArguments {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    PreparedStatement stmt;

    StatementContext ctx = new StatementContext();

    @Test
    public void testBind() throws Exception
    {
        Map<String, Object> args = Collections.singletonMap("foo", BigDecimal.ONE);
        applyArgument(5, new MapArguments(args).find("foo").get());

        verify(stmt).setBigDecimal(5, BigDecimal.ONE);
    }

    @Test
    public void testNullBinding() throws Exception
    {
        Map<String, Object> args = Collections.singletonMap("foo", null);
        applyArgument(3, new MapArguments(args).find("foo").get());

        verify(stmt).setNull(3, Types.NULL);
    }

    @SuppressWarnings("unchecked")
    private void applyArgument(int position, BoundArgument argument) throws SQLException {
        Argument<Object> arg = (Argument<Object>) ctx.findArgumentFor(argument.getType()).get();
        arg.apply(stmt, position, argument.getValue(), ctx);
    }
}
