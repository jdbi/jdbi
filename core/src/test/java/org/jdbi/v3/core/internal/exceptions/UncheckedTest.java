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
package org.jdbi.v3.core.internal.exceptions;

import java.sql.SQLException;
import java.util.function.Function;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UncheckedTest {
    @Test
    public void hidesThrowsClause() {
        CheckedFunction<String, Object> throwing = UncheckedTest::failToGet;

        assertThatThrownBy(() -> throwing.apply(""))
            .describedAs("throws a checked exception")
            .isInstanceOf(SQLException.class);

        Function<String, Object> muted = Unchecked.function(throwing);

        assertThat(muted)
            .describedAs("does not declare a checked exception")
            .isInstanceOf(Function.class);

        assertThatThrownBy(() -> muted.apply(""))
            .describedAs("still throws a checked exception")
            .isInstanceOf(SQLException.class);
    }

    private static Object failToGet(String key) throws SQLException {
        throw new SQLException();
    }
}
