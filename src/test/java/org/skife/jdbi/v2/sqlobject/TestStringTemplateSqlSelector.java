package org.skife.jdbi.v2.sqlobject;

import org.junit.Test;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.stringtemplate.StringTemplate3Locator;
import org.skife.jdbi.v2.tweak.StatementLocator;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestStringTemplateSqlSelector
{
    @Test
    public void testFoo() throws Exception
    {
        assertThat(Wombat.class.getName(),
                   equalTo("org.skife.jdbi.v2.sqlobject.TestStringTemplateSqlSelector$Wombat"));

    }

    @Test
    public void testBar() throws Exception
    {
        StringTemplate3Locator s = Wombat.class.getAnnotation(StringTemplate3Locator.class);
        StringTemplate3Locator.LocatorFactory lf = StringTemplate3Locator.LocatorFactory.class.newInstance();

        StatementLocator l = lf.create(s, Wombat.class, null);

        StatementContext ctx = new StatementContext(new HashMap<String, Object>())
        {
            { this.setAttribute("name", "Brian"); }
        };
        String st = l.locate("kangaroo", ctx);
        System.out.println(st);
    }


    @StringTemplate3Locator
    static class Wombat
    {

    }
}
