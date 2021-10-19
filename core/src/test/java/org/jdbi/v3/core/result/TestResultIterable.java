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

import java.util.stream.Collectors;

import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestResultIterable {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    @Test
    public void testMapIntToDouble() {
        h2Extension.getJdbi().useHandle(h -> {
            h.execute("CREATE TABLE numbers (u INT)");
            h.execute("INSERT INTO numbers VALUES (0), (1), (2), (3), (4)");
        });

        assertThat(h2Extension.getSharedHandle().createQuery("select * from numbers")
            .mapTo(int.class)
            .map(i -> i * 1.5)
            .list())
            .containsExactly(0.0, 1.5, 3.0, 4.5, 6.0);
    }

    @Test
    public void testMapStringToReverse() {
        h2Extension.getJdbi().useHandle(h -> {
            h.execute("CREATE TABLE strings (v TEXT)");
            h.execute("INSERT INTO strings VALUES ('foo'), ('bar'), ('baz'), ('buz'), ('qux')");
        });

        assertThat(h2Extension.getSharedHandle().createQuery("select * from strings")
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

    @Test
    public void testCollectSuper() {
        h2Extension.getJdbi().useHandle(h -> {
            h.execute("CREATE TABLE numbers (id INT)");
            h.execute("INSERT INTO numbers VALUES (1), (2), (3)");
        });

        assertThat(h2Extension.getSharedHandle().createQuery("select * from numbers order by 1")
            .map((rs, ctx) -> "ID: " + rs.getInt("id"))
            .collect(Collectors.joining("\n")))
            .isEqualTo("ID: 1\nID: 2\nID: 3");
    }
}
