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

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizer;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator.LocatorFactory;

public class TestLocatorCaching {
    @Test
    public void testCaching() {
        LocatorFactory lf = new LocatorFactory();

        SqlStatementCustomizer locator1 = lf.createForType(
                Kombucha.class.getAnnotation(UseStringTemplate3StatementLocator.class),
                Kombucha.class);

        SqlStatementCustomizer locator2 = lf.createForType(
                Kombucha.class.getAnnotation(UseStringTemplate3StatementLocator.class),
                Kombucha.class);

        assertSame(locator1, locator2);
    }

    @Test
    public void testNoCaching() {
        LocatorFactory lf = new LocatorFactory();

        SqlStatementCustomizer locator1 = lf.createForType(
                SuperDrink.class.getAnnotation(UseStringTemplate3StatementLocator.class),
                SuperDrink.class);

        SqlStatementCustomizer locator2 = lf.createForType(
                SuperDrink.class.getAnnotation(UseStringTemplate3StatementLocator.class),
                SuperDrink.class);

        assertNotSame(locator1, locator2);
    }
}
