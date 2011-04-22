package org.skife.jdbi.v2.sqlobject;

import org.junit.Test;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.stringtemplate.StringTemplateSqlSelector;
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
        StringTemplateSqlSelector s = Wombat.class.getAnnotation(StringTemplateSqlSelector.class);
        StringTemplateSqlSelector.LocatorFactory lf = StringTemplateSqlSelector.LocatorFactory.class.newInstance();
        StatementContext ctx = new StatementContext(new HashMap<String, Object>())
        {
            { this.setAttribute("name", "Brian"); }
        };
        StatementLocator l = lf.create(s, Wombat.class, ctx);

        String st = l.locate("kangaroo", ctx);
        System.out.println(st);
    }


    @StringTemplateSqlSelector
    static class Wombat
    {

    }
}
