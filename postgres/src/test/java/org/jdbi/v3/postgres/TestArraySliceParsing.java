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
package org.jdbi.v3.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import org.jdbi.v3.testing.JdbiRule;
import org.junit.Rule;
import org.junit.Test;

public class TestArraySliceParsing {
    @Rule
    public JdbiRule db = JdbiRule.embeddedPostgres();

    @Test
    public void testArraySliceFull() {
        assertThat(db.getHandle().createQuery(
                    "select col[2:2] from (values ('{1,2,3}'::int[])) as tbl(col)")
                .mapTo(int[].class)
                .findOnly())
            .isEqualTo(new int[] {2});
    }

    @Test
    public void testArraySliceLower() {
        assertThat(db.getHandle().createQuery(
                    "select col[2:] from (values ('{1,2,3}'::int[])) as tbl(col)")
                .mapTo(int[].class)
                .findOnly())
            .isEqualTo(new int[] {2, 3});
    }

    @Test
    public void testArraySliceUpper() {
        assertThat(db.getHandle().createQuery(
                    "select col[:2] from (values ('{1,2,3}'::int[])) as tbl(col)")
                .mapTo(int[].class)
                .findOnly())
            .isEqualTo(new int[] {1, 2});
    }

    @Test
    public void testNumeralName() {
        assertThat(db.getHandle().createQuery("values(:2)")
                    .bind("2", 42)
                    .mapTo(int.class)
                    .findOnly())
            .isEqualTo(42);
    }
}
