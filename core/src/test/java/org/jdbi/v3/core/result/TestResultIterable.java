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
package org.jdbi.v3.core.result;

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestResultIterable {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    @Test
    public void testMapIntToDouble() {
        dbRule.getJdbi().useHandle(h -> {
            h.execute("CREATE TABLE numbers (u INT)");
            h.execute("INSERT INTO numbers VALUES (0), (1), (2), (3), (4)");
        });

        assertThat(dbRule.getSharedHandle().createQuery("select * from numbers")
            .mapTo(int.class)
            .map(i -> i * 1.5)
            .list())
            .containsExactly(0.0, 1.5, 3.0, 4.5, 6.0);
    }

    @Test
    public void testMapStringToReverse() {
        dbRule.getJdbi().useHandle(h -> {
            h.execute("CREATE TABLE strings (v TEXT)");
            h.execute("INSERT INTO strings VALUES ('foo'), ('bar'), ('baz'), ('buz'), ('qux')");
        });

        assertThat(dbRule.getSharedHandle().createQuery("select * from strings")
            .mapTo(String.class)
            .map(this::reverse)
            .list())
            .containsExactly("oof", "rab", "zab", "zub", "xuq");
    }

    private String reverse(String input) {
        StringBuilder b = new StringBuilder(input.length());
        for (int i = input.length() - 1; i >= 0; i--) {
            b.append(input.charAt(i));
        }
        return b.toString();
    }
}
