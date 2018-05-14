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
package org.jdbi.v3.core.argument;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.rule.PgDatabaseRule;
import org.junit.Rule;
import org.junit.Test;

public class TestCollectionArguments {
    @Rule
    public PgDatabaseRule db = new PgDatabaseRule();

    @Test
    public void testBindTypeErased() throws Exception {
        try (final Handle h = db.openHandle()) {
            assertThatThrownBy(() ->
                h.execute("SELECT * FROM something WHERE id = ANY(:ids)", Collections.singleton(1)))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("No type parameters found");
        }
    }
}
