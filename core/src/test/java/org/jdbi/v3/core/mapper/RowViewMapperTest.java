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

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RowViewMapperTest {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Test
    public void testRowViewMap() {
        Handle h = db.getSharedHandle();
        final Map<Integer, String> expected = new HashMap<>();
        expected.put(1, "SFO");
        expected.put(2, "OAK");
        expected.put(3, "YYZ");
        h.execute("create table airport (id int, code varchar)");
        PreparedBatch batch = h.prepareBatch("insert into airport (id, code) values (:id, :code)");
        expected.forEach((id, code) -> batch.bind("id", id).bind("code", code).add());
        batch.execute();
        assertThat(h.createQuery("select id, code from airport")
                .map(rowView -> new AbstractMap.SimpleEntry<>(rowView.getColumn("id", Integer.class), rowView.getColumn("code", String.class)))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue)))
            .isEqualTo(expected);
    }
}
