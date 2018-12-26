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
package org.jdbi.v3.core.mapper;

import java.util.Optional;
import java.util.OptionalInt;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class MapOptionalTest {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Test
    public void testMapOptional() {
        final Handle h = db.getSharedHandle();
        h.execute("insert into something(intValue, name) values(1, 'Duke')");
        h.execute("insert into something(intValue, name) values(null, null)");

        assertThat(h.createQuery("select * from something order by id")
                .map(ConstructorMapper.of(OptionalBean.class))
                .list())
            .extracting("intValue", "name")
            .containsExactly(
                tuple(OptionalInt.of(1), Optional.of("Duke")),
                tuple(OptionalInt.empty(), Optional.empty()));
    }

    public static class OptionalBean {
        public final OptionalInt intValue;
        public final Optional<String> name;
        public OptionalBean(OptionalInt intValue, Optional<String> name) {
            this.intValue = intValue;
            this.name = name;
        }
    }
}
