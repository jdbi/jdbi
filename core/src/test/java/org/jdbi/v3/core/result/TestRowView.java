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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.meta.Beta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestRowView {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    @BeforeEach
    public void setUp() {
        Handle h = h2Extension.getSharedHandle();
        h.execute("CREATE TABLE test (a INT)");
        for (int a = 0; a < 5; a++) {
            h.execute("INSERT INTO test VALUES (?)", a);
        }
    }

    @Test
    public void testRowViewClass() {
        assertThat(
            h2Extension.getSharedHandle().createQuery("SELECT * FROM test")
                .reduceRows(0, (a, rv) -> a + rv.getColumn("a", Integer.class)))
            .isEqualTo(10);
    }

    @Test
    public void testRowViewQualifiedType() {
        assertThatThrownBy(() ->
            h2Extension.getSharedHandle().createQuery("SELECT * FROM test")
                .reduceRows(0, (a, rv) -> a + rv.getColumn("a", QualifiedType.of(int.class).with(Beta.class))))
            .isInstanceOf(NoSuchMapperException.class);
    }
}
