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
import java.sql.Types;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class TestBeanArguments
{
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    PreparedStatement stmt;

    StatementContext ctx = new StatementContext();

    @Test
    public void testBindBare() throws Exception
    {
        Object bean = new Object() {
            @SuppressWarnings("unused")
            public BigDecimal getFoo() {
                return BigDecimal.ONE;
            }
        };

        new BeanPropertyArguments("", bean, ctx).find("foo").get().apply(5, stmt, null);

        verify(stmt).setBigDecimal(5, BigDecimal.ONE);
    }

    @Test
    public void testBindNull() throws Exception
    {
        Object bean = new Object() {
            @SuppressWarnings("unused")
            public BigDecimal getFoo() {
                return null;
            }
        };

        new BeanPropertyArguments("", bean, ctx).find("foo").get().apply(3, stmt, null);

        verify(stmt).setNull(3, Types.NUMERIC);
    }

    @Test
    public void testBindPrefix() throws Exception
    {
        Object bean = new Object() {
            @SuppressWarnings("unused")
            public String getBar() {
                return "baz";
            }
        };

        new BeanPropertyArguments("foo", bean, ctx).find("foo.bar").get().apply(3, stmt, null);

        verify(stmt).setString(3, "baz");
    }
}
