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
package org.jdbi.v3.guava;

import java.util.UUID;

import com.google.common.collect.ImmutableList;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.rule.PgDatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestGuavaMappers {
    @Rule
    public PgDatabaseRule dbRule = new PgDatabaseRule().withPlugin(new GuavaPlugin());

    private Handle h;

    @Before
    public void setUp() {
        dbRule.getJdbi()
                .registerArrayType(Integer.class, "integer")
                .registerArrayType(UUID.class, "uuid");
        h = dbRule.openHandle();
        h.useTransaction(th -> {
            th.execute("DROP TABLE IF EXISTS arrays");
            th.execute("CREATE TABLE arrays (u UUID[], i INT[])");
        });
    }

    @Test
    public void testUuidImmutableList() {
        UUID[] testUuids = new UUID[]{
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
        };

        h.execute("INSERT INTO arrays (u) VALUES(?)", (Object) testUuids);
        ImmutableList<UUID> list = h.createQuery("SELECT u FROM arrays")
                .mapTo(new GenericType<ImmutableList<UUID>>() {})
                .findOnly();
        assertThat(list).contains(testUuids);
    }

    @Test
    public void testIntegerImmutableList() {
        Integer[] testInts = new Integer[]{
                5, 4, -6, 1, 9, Integer.MAX_VALUE, Integer.MIN_VALUE
        };

        h.execute("INSERT INTO arrays (i) VALUES(?)", (Object) testInts);
        ImmutableList<Integer> list = h.createQuery("SELECT i FROM arrays")
                .mapTo(new GenericType<ImmutableList<Integer>>() {})
                .findOnly();
        assertThat(list).contains(testInts);
    }
}
