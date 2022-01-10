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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.google.common.base.Preconditions.checkState;
import static org.jdbi.v3.core.junit5.H2DatabaseExtension.SOMETHING_INITIALIZER;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Test1968Batch {

    @RegisterExtension
    private final H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(SOMETHING_INITIALIZER);

    public enum FooType {
        OTHER, SOMETHING_ELSE
    }

    @Test
    public void testNullLast() {
        List<Map<String, Object>> data = ImmutableList.of(
            createMap("id", 1L, "name", FooType.OTHER),
            createMap("id", 2L, "name", FooType.SOMETHING_ELSE),
            createMap("id", 3L, "name", null)
        );

        doTest(data);
    }

    @Test
    public void testNullFirst() {
        List<Map<String, Object>> data = ImmutableList.of(
            createMap("id", 1L, "name", null),
            createMap("id", 2L, "name", FooType.SOMETHING_ELSE),
            createMap("id", 3L, "name", FooType.OTHER)
        );

        doTest(data);
    }

    @Test
    public void testColumnMissing() {
        List<Map<String, Object>> data = ImmutableList.of(
            createMap("id", 1L),
            createMap("id", 2L, "name", FooType.SOMETHING_ELSE),
            createMap("id", 3L, "name", FooType.OTHER)
        );

        doTest(data);
    }

    @Test
    public void testColumnMissingLater() {
        List<Map<String, Object>> data = ImmutableList.of(
            createMap("id", 1L, "name", FooType.SOMETHING_ELSE),
            createMap("id", 2L),
            createMap("id", 3L, "name", FooType.OTHER)
        );

        doTest(data);
    }

    private void doTest(List<Map<String, Object>> data) {
        Handle h = h2Extension.openHandle();
        PreparedBatch b = h.prepareBatch("INSERT INTO something (id, name) values (:id, :name)");
        data.forEach(m -> b.bindMap(m).add());

        int[] result = b.execute();

        assertEquals(3, result.length);

        List<Map<String, Object>> r = h.createQuery("select id, name from something order by id").mapToMap().list();

        assertEquals(data.size(), r.size());
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> original = data.get(i);
            Map<String, Object> loaded = r.get(i);
            assertEquals(original.get("id"), loaded.get("id"));
            FooType loadedEnum = loaded.get("name") == null ? null : FooType.valueOf((String) loaded.get("name"));
            assertEquals(original.get("name"), loadedEnum);
        }
    }

    private static Map<String, Object> createMap(Object... args) {
        checkState(args.length % 2 == 0, "args has %s arguments (must be even!)", args.length);
        Map<String, Object> result = new HashMap<>();

        for (int i = 0; i < args.length / 2; i++) {
            result.put(args[i * 2].toString(), args[i * 2 + 1]);
        }

        return result;
    }
}
