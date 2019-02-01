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
package org.jdbi.v3.core.statement;

import java.sql.SQLException;

import org.h2.jdbc.JdbcPreparedStatement;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class TestDefineNamedBindings {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Test
    public void testDefineStrings() {
        assertThat(
            db.getSharedHandle().createQuery("select <a> from values(:a) union all select <b> from values(:b)")
                .defineNamedBindings()
                .bindBean(new DefinedBean())
                .mapTo(boolean.class)
                .list())
        .containsExactly(true, false);
    }

    public static class DefinedBean {
        public String getA() {
            return "x";
        }

        public String getB() {
            return null;
        }
    }

    @Test
    public void testIncompatibleWithUnwrap() {
        Throwable thrown = catchThrowable(() ->
            db.getSharedHandle().createQuery("select <a> from values (:a)")
                .defineNamedBindings()
                .bind("a", (p, s, c) -> s.unwrap(JdbcPreparedStatement.class).setString(p, "x"))
                .mapTo(boolean.class)
                .findFirst());
        assertThat(thrown)
            .isNotNull()
            .hasCauseInstanceOf(SQLException.class);
        assertThat(thrown.getCause())
            .hasMessageContaining("DefineNamedBindings is incompatible with arguments that rely on java.sql.Wrapper.unwrap");
    }

}
