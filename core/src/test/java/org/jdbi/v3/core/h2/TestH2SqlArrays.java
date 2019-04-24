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
package org.jdbi.v3.core.h2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestH2SqlArrays {
    private static final GenericType<List<UUID>> UUID_LIST = new GenericType<List<UUID>>() {};
    private static final GenericType<ArrayList<UUID>> UUID_ARRAYLIST = new GenericType<ArrayList<UUID>>() {};
    private static final GenericType<Set<UUID>> UUID_SET = new GenericType<Set<UUID>>() {};
    private static final GenericType<HashSet<UUID>> UUID_HASHSET = new GenericType<HashSet<UUID>>() {};
    private static final GenericType<LinkedHashSet<UUID>> UUID_LINKEDHASHSET = new GenericType<LinkedHashSet<UUID>>() {};

    private static final String U_SELECT = "SELECT u FROM uuids";
    private static final String U_INSERT = "INSERT INTO uuids VALUES(:u)";

    @ClassRule
    public static H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new H2DatabasePlugin());

    private Handle h;

    @Before
    public void setUp() {
        h = dbRule.getSharedHandle();
        h.useTransaction(th -> {
            th.execute("DROP TABLE IF EXISTS uuids");
            th.execute("CREATE TABLE uuids (u ARRAY)");
        });
    }

    private final UUID[] testUuids = new UUID[] {
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
    };

    @Test
    public void testUuidArray() {
        assertThat(
            h.createUpdate(U_INSERT)
                .bind("u", testUuids)
                .execute())
            .isEqualTo(1);
        assertThat(
            h.createQuery(U_SELECT)
                .mapTo(UUID[].class)
                .one())
            .containsExactly(testUuids);
    }

    @Test
    public void testUuidList() {
        assertThat(
            h.createUpdate(U_INSERT)
                .bindByType("u", Arrays.asList(testUuids), UUID_LIST)
                .execute())
            .isEqualTo(1);
        assertThat(
            h.createQuery(U_SELECT)
                .mapTo(UUID_LIST)
                .one())
            .containsExactly(testUuids);
    }

    @Test
    public void testUuidArrayList() {
        assertThat(
            h.createUpdate(U_INSERT)
                .bindByType("u", new ArrayList<>(Arrays.asList(testUuids)), UUID_LIST)
                .execute())
            .isEqualTo(1);
        assertThat(
            h.createQuery(U_SELECT)
                .mapTo(UUID_ARRAYLIST)
                .one())
            .containsExactly(testUuids);
    }

    @Test
    public void testUuidHashSet() {
        assertThat(
            h.createUpdate(U_INSERT)
                .bindByType("u", new HashSet<>(Arrays.asList(testUuids)), UUID_SET)
                .execute())
            .isEqualTo(1);
        assertThat(
            h.createQuery(U_SELECT)
                .mapTo(UUID_HASHSET)
                .one())
            .containsExactlyInAnyOrder(testUuids);
    }

    @Test
    public void testUuidLinkedHashSet() {
        assertThat(
            h.createUpdate(U_INSERT)
                .bindByType("u", new LinkedHashSet<>(Arrays.asList(testUuids)), UUID_SET)
                .execute())
            .isEqualTo(1);
        assertThat(
            h.createQuery(U_SELECT)
                .mapTo(UUID_LINKEDHASHSET)
                .one())
            .isInstanceOf(LinkedHashSet.class)
            .containsExactly(testUuids);
    }

    @Test
    public void testEnumArrays() {
        Handle h = dbRule.openHandle();

        GenericType<List<TestEnum>> testEnumList = new GenericType<List<TestEnum>>() {};

        assertThat(h.select("select ?")
            .bindByType(0, Arrays.asList(TestEnum.values()), testEnumList)
            .mapTo(testEnumList).one())
            .containsExactly(TestEnum.values());
    }

    public enum TestEnum {
        FOO, BAR, BAZ
    }

}
