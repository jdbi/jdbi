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
package org.jdbi.v3.testing.junit5;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
@ExtendWith(JdbiOtjPostgresExtension.class)
public class JdbiOtjPostgresExtendWithTest {

    @Test
    public void testJdbiIsAlive(Jdbi jdbi) {
        Integer one = jdbi.withHandle(h -> h.createQuery("select 1").mapTo(Integer.class).one());

        assertThat(one).isOne();
    }

    @Test
    public void testHandleIsAlive(Handle handle) {
        Integer one = handle.createQuery("select 1").mapTo(Integer.class).one();

        assertThat(one).isOne();
    }
}
