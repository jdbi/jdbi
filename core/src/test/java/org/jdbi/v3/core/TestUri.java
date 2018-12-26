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
package org.jdbi.v3.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Rule;
import org.junit.Test;

public class TestUri {
    private static final URI TEST_URI = URI.create("http://example.invalid/wat.jpg");
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    @Test
    public void testUri() {
        Handle h = dbRule.openHandle();
        h.createUpdate("insert into something (id, name) values (1, :uri)")
            .bind("uri", TEST_URI).execute();

        assertThat(h.createQuery("SELECT name FROM something")
            .mapTo(URI.class)
            .findOnly()).isEqualTo(TEST_URI);
    }
}
