package org.skife.jdbi.v2;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.skife.jdbi.v2.tweak.Argument;

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
