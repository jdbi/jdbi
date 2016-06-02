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
package org.jdbi.v3.sqlobject.stringtemplate;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.jdbi.v3.sqlobject.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.stringtemplate.UseStringTemplate3StatementLocator.LocatorFactory;
import org.junit.Test;

public class TestLocatorCaching {
    @Test
    public void testCaching() {
        LocatorFactory lf = new LocatorFactory();

        SqlStatementCustomizer locator1 = lf.createForType(
                AllowCache.class.getAnnotation(UseStringTemplate3StatementLocator.class),
                AllowCache.class);

        SqlStatementCustomizer locator2 = lf.createForType(
                AllowCache.class.getAnnotation(UseStringTemplate3StatementLocator.class),
                AllowCache.class);

        assertSame(locator1, locator2);
    }

    @Test
    public void testNoCaching() {
        LocatorFactory lf = new LocatorFactory();

        SqlStatementCustomizer locator1 = lf.createForType(
                DisallowCache.class.getAnnotation(UseStringTemplate3StatementLocator.class),
                DisallowCache.class);

        SqlStatementCustomizer locator2 = lf.createForType(
                DisallowCache.class.getAnnotation(UseStringTemplate3StatementLocator.class),
                DisallowCache.class);

        assertNotSame(locator1, locator2);
    }

    @UseStringTemplate3StatementLocator(cacheable = true)
    public interface AllowCache { }

    @UseStringTemplate3StatementLocator(cacheable = false)
    public interface DisallowCache { }
}
