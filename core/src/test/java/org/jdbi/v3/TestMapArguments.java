/*
 * Copyright (C) 2004 - 2013 Brian McCallister
 *
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

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.jdbi.v3.BigDecimalArgument;
import org.jdbi.v3.ConcreteStatementContext;
import org.jdbi.v3.Foreman;
import org.jdbi.v3.MapArguments;
import org.jdbi.v3.StatementContext;
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
        StatementContext ctx = new ConcreteStatementContext(new HashMap<String, Object>());
        MapArguments mapArguments = new MapArguments(foreman, ctx, args);
        Argument argument = mapArguments.find("foo");
        assertThat(argument, instanceOf(BigDecimalArgument.class));
	}

}
