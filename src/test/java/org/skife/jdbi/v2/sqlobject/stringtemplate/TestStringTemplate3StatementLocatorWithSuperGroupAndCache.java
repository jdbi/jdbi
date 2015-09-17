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
package org.skife.jdbi.v2.sqlobject.stringtemplate;

import org.antlr.stringtemplate.StringTemplateGroup;
import org.junit.Assert;
import org.junit.Test;
import org.skife.jdbi.v2.StatementContext;

import java.util.HashMap;

public class TestStringTemplate3StatementLocatorWithSuperGroupAndCache
{
    @Test
    public void testSuperTemplate() throws Exception
    {
        final StringTemplate3StatementLocator locator = StringTemplate3StatementLocator.builder(Kombucha.class)
                        .withSuperGroup(SuperDrink.class)
                        .withErrorListener(StringTemplateGroup.DEFAULT_ERROR_LISTENER)
                        .allowImplicitTemplateGroup()
                        .treatLiteralsAsTemplates()
                        .shouldCache()
                        .build();

        // Test statement locator from child template group and verify templates table_name got correctly evaluated
        final StatementContext ctx = new TestingStatementContext(new HashMap<String, Object>());
        final String getIngredients = locator.locate("getIngredients", ctx);
        Assert.assertEquals("select tea\n" +
            ", mushroom\n" +
            ", sugar from kombucha;", getIngredients);

        // Test statement locator from base template group
        final String awesomeness = locator.locate("awesomeness", ctx);
        Assert.assertEquals("awesomeness;", awesomeness);
    }

    @Test
    public void testLocatorWithAttributes() throws Exception
    {
        final StringTemplate3StatementLocator locator = StringTemplate3StatementLocator.builder(Kombucha.class)
                        .withSuperGroup(SuperDrink.class)
                        .withErrorListener(StringTemplateGroup.DEFAULT_ERROR_LISTENER)
                        .allowImplicitTemplateGroup()
                        .treatLiteralsAsTemplates()
                        .shouldCache()
                        .build();

        final StatementContext ctx = new TestingStatementContext(new HashMap<String, Object>());
        ctx.setAttribute("historyTableName", "superDrink");

        // Test the attributes get correctly evaluated
        final String getFromHistoryTableName = locator.locate("getFromHistoryTableName", ctx);
        Assert.assertEquals("select tea\n" +
            ", mushroom\n" +
            ", sugar from superDrink;", getFromHistoryTableName);

        // Verify we cached the template evaluation
        Assert.assertTrue(StringTemplate3StatementLocator.templateCached(Kombucha.class, SuperDrink.class));

        // Try another time with same key-- to verify the attributes of the StringTemplate got reset correctly and we don't end up with twice the name of the table
        // (By default StringTemplate attributes value are appended when if we set them multiple times)

        final String getFromHistoryTableNameAgain = locator.locate("getFromHistoryTableName", ctx);
        Assert.assertEquals("select tea\n" +
            ", mushroom\n" +
            ", sugar from superDrink;", getFromHistoryTableNameAgain);
    }
}
