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
package org.jdbi.v3;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.jdbi.v3.tweak.Argument;
import org.junit.Test;

public class TestMapArguments
{

    @Test
    public void testBind() throws Exception
    {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("foo", BigDecimal.ONE);
        Foreman foreman = new Foreman();
        StatementContext ctx = new ConcreteStatementContext();
        MapArguments mapArguments = new MapArguments(foreman, ctx, args);
        Argument argument = mapArguments.find("foo");
        assertThat(argument, instanceOf(BigDecimalArgument.class));
    }

    @Test
    public void testNullBinding() throws Exception
    {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("foo", null);
        Foreman foreman = new Foreman();
        StatementContext ctx = new ConcreteStatementContext();
        MapArguments mapArguments = new MapArguments(foreman, ctx, args);
        Argument argument = mapArguments.find("foo");
        assertThat(argument, instanceOf(ObjectArgument.class));
    }
}
