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
package org.jdbi.v3.core.nothing;

import org.jdbi.v3.core.rule.DatabaseRule;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestSupportNothing {
    @Rule
    public DatabaseRule db = new H2DatabaseRule();

    @Before
    public void before() {
        db.getJdbi().useHandle(h -> h.execute("create table foo(id varchar)"));
    }

    @Test
    public void controlGroup() {
        String id = "one";

        db.getJdbi().useHandle(h -> h.createUpdate("insert into foo(id) values(?)").bind(0, id).execute());

        String out = db.getJdbi().withHandle(h -> h.createQuery("select id from foo").mapTo(String.class).findOnly());
        assertThat(out).isEqualTo(id);
    }

    @Test
    public void testArgument() {
        db.getJdbi().installPlugin(new SupportNothingPlugin());

        assertThatThrownBy(() -> db.getJdbi().useHandle(h -> h.createUpdate("insert into foo(id) values(?)").bind(0, "one")))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("You've installed ");
    }

    @Test
    public void testMapper() {
        db.getJdbi().useHandle(h -> h.createUpdate("insert into foo(id) values(?)").bind(0, "one").execute());

        db.getJdbi().installPlugin(new SupportNothingPlugin());

        assertThatThrownBy(() -> db.getJdbi().withHandle(h -> h.createQuery("select id from foo").mapTo(String.class)))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("You've installed ");
    }
}
