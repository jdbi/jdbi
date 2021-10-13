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

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Collections;
import java.util.Map;

import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementContextAccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * @deprecated now we just bind arguments directly as named
 */
@Deprecated
@ExtendWith(MockitoExtension.class)
public class TestMapArguments {

    @Mock
    PreparedStatement stmt;

    StatementContext ctx = StatementContextAccess.createContext();

    @Test
    public void testBind() throws Exception {
        Map<String, Object> args = Collections.singletonMap("foo", BigDecimal.ONE);
        new MapArguments(args).find("foo", ctx).get().apply(5, stmt, null);

        verify(stmt).setBigDecimal(5, BigDecimal.ONE);
    }

    @Test
    public void testNullBinding() throws Exception {
        Map<String, Object> args = Collections.singletonMap("foo", null);
        new MapArguments(args).find("foo", ctx).get().apply(3, stmt, null);

        verify(stmt).setNull(3, Types.OTHER);
    }
}
