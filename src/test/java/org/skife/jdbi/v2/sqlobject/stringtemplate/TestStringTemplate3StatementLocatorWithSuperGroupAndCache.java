package org.skife.jdbi.v2.sqlobject.stringtemplate;

import java.util.HashMap;
import java.util.Map;

import org.skife.jdbi.v2.SomeStatementContext;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.TestSuperDrink;
import org.skife.jdbi.v2.sqlobject.Testkombucha;
import org.skife.jdbi.v2.sqlobject.stringtemplate.StringTemplate3StatementLocator.LocatorKey;

import junit.framework.TestCase;


public class TestStringTemplate3StatementLocatorWithSuperGroupAndCache extends TestCase {


    public final static class StringTemplate3StatementLocatorWithCacheVisibility extends StringTemplate3StatementLocator {

        public StringTemplate3StatementLocatorWithCacheVisibility(final Class baseClass, final boolean allowImplicitTemplateGroup, final boolean treatLiteralsAsTemplates, final Class superTemplateGroupClass) {
            super(baseClass, allowImplicitTemplateGroup, treatLiteralsAsTemplates, superTemplateGroupClass);
        }

        public boolean isTemplateValueCached(final String templateName, Map<String, Object> templateAttributes) {
            final LocatorKey key = new LocatorKey(templateName, templateAttributes);
            return cachedEvaluatedTemplate.containsKey(key);
        }
    }

    public void testSuperTemplate() throws Exception {

        final StringTemplate3StatementLocator locator = new StringTemplate3StatementLocator(Testkombucha.class, true, true, TestSuperDrink.class);
        locator.setCachedEvaluatedTemplate();

        // Test statement locator from child template group and verify templates table_name got correctly evaluated
        final StatementContext ctx = new SomeStatementContext(new HashMap<String, Object>());
        final String getIngredients = locator.locate("getIngredients", ctx);
        assertEquals("select tea\n" +
                     ", mushroom\n" +
                     ", sugar from kombucha;", getIngredients);

        // Test statement locator from base template group
        final String awesomeness = locator.locate("awesomeness", ctx);
        assertEquals("awesomeness;", awesomeness);
    }

    public void testLocatorWithAttributes() throws Exception {

        final StringTemplate3StatementLocatorWithCacheVisibility locator = new StringTemplate3StatementLocatorWithCacheVisibility(Testkombucha.class, true, true, TestSuperDrink.class);
        locator.setCachedEvaluatedTemplate();
        final StatementContext ctx = new SomeStatementContext(new HashMap<String, Object>());
        ctx.setAttribute("historyTableName", "superDrink");

        // Test the attributes get correctly evaluated
        final String getFromHistoryTableName = locator.locate("getFromHistoryTableName", ctx);
        assertEquals("select tea\n" +
                     ", mushroom\n" +
                     ", sugar from superDrink;", getFromHistoryTableName);

        // Verify we cached the template evaluation
        assertTrue(locator.isTemplateValueCached("getFromHistoryTableName", ctx.getAttributes()));

        // Try another time with same key-- to verify the attributes of the StringTemplate got reset correctly and we don't end up with twice the name of the table
        // (By default StringTemplate attributes value are appended when if we set them multiple times)

        final String getFromHistoryTableNameAgain = locator.locate("getFromHistoryTableName", ctx);
        assertEquals("select tea\n" +
                     ", mushroom\n" +
                     ", sugar from superDrink;", getFromHistoryTableNameAgain);
    }

    public void testLocatorKey() {

        // Test same LocatorKey with no attributes are indeed seen as being equal
        final LocatorKey key1 = new LocatorKey("foo", new HashMap<String, Object>());
        final LocatorKey key1Identical = new LocatorKey("foo", new HashMap<String, Object>());
        assertTrue(key1.equals(key1Identical));

        // Test different LocatorKey with no attributes are indeed seen as NOT being equal
        final LocatorKey key1Different = new LocatorKey("bar", new HashMap<String, Object>());
        assertFalse(key1.equals(key1Different));

        // Test same LocatorKey with attributes are indeed seen as being equal
        final HashMap<String, Object> attr2 = new HashMap<String, Object>();
        attr2.put("table_name", "yoyo");
        final LocatorKey key2 = new LocatorKey("the universe is small", attr2);

        final HashMap<String, Object> attr2Identical = new HashMap<String, Object>();
        attr2Identical.put("table_name", "yoyo");
        final LocatorKey key2Identical = new LocatorKey("the universe is small", attr2Identical);
        assertTrue(key2.equals(key2Identical));

        // Test different LocatorKey with attributes are indeed seen as NOT being equal
        final HashMap<String, Object> attr2Different = new HashMap<String, Object>();
        attr2Different.put("table_name", "coincoin");
        final LocatorKey key2Different = new LocatorKey("the universe is small", attr2Different);
        assertFalse(key2.equals(key2Different));
    }
}
