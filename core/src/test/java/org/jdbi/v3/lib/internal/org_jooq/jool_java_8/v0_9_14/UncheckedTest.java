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
package org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14;

import java.io.IOException;
import java.util.function.Supplier;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UncheckedTest {
    @Test
    public void testThrowChecked() {
        Runnable noThrowsClause = UncheckedTest::throwWithoutThrowsClause;

        assertThat(noThrowsClause)
            .describedAs("is an unchecked method")
            .isInstanceOf(Runnable.class);

        assertThatThrownBy(noThrowsClause::run)
            .describedAs("has thrown a checked exception anyway")
            .isExactlyInstanceOf(Throwable.class)
            .hasMessage("foo");
    }

    @Test
    public void testSupplier() {
        Supplier<String> supplier = Unchecked.supplier(UncheckedTest::harmlessGetterWithThrowsClause);

        assertThat(supplier)
            .describedAs("does not declare a checked exception")
            .isInstanceOf(Supplier.class);
    }

    private static void throwWithoutThrowsClause() {
        // note that this call does not force a throws clause
        throw Unchecked.throwChecked(new Throwable("foo"));
    }

    @SuppressWarnings("ConstantConditions")
    private static String harmlessGetterWithThrowsClause() throws IOException {
        if (false) {
            // just to force the throws clause
            throw new IOException();
        } else {
            return "foo";
        }
    }
}
