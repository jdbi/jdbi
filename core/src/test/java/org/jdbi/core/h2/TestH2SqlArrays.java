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
package org.jdbi.core.h2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jdbi.core.generic.GenericType;
import org.jdbi.core.internal.testing.H2DatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestH2SqlArrays {

    private static final GenericType<List<UUID>> UUID_LIST = new GenericType<List<UUID>>() {};
    private static final GenericType<ArrayList<UUID>> UUID_ARRAYLIST = new GenericType<ArrayList<UUID>>() {};
    private static final GenericType<Set<UUID>> UUID_SET = new GenericType<Set<UUID>>() {};
    private static final GenericType<HashSet<UUID>> UUID_HASHSET = new GenericType<HashSet<UUID>>() {};
    private static final GenericType<LinkedHashSet<UUID>> UUID_LINKEDHASHSET = new GenericType<LinkedHashSet<UUID>>() {};

    private static final String U_SELECT = "SELECT u FROM uuids";
    private static final String U_INSERT = "INSERT INTO uuids VALUES(:u)";

    @RegisterExtension
    public static H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(h ->
        h.useTransaction(th -> {
            th.execute("DROP TABLE IF EXISTS uuids");
            th.execute("CREATE TABLE uuids (u UUID ARRAY)");
        }));

    private final UUID[] testUuids = new UUID[]{
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
    };

    @Test
    public void testUuidArray() {
        assertThat(
            h2Extension.getSharedHandle()
                .createUpdate(U_INSERT)
                .bindArray("u", testUuids)
                .execute())
            .isOne();
        assertThat(
            h2Extension.getSharedHandle()
                .createQuery(U_SELECT)
                .mapTo(UUID[].class)
                .one())
            .containsExactly(testUuids);
    }

    @Test
    public void testUuidList() {
        assertThat(
            h2Extension.getSharedHandle()
                .createUpdate(U_INSERT)
                .bindArray("u", UUID.class, Arrays.asList(testUuids))
                .execute())
            .isOne();
        assertThat(
            h2Extension.getSharedHandle()
                .createQuery(U_SELECT)
                .mapTo(UUID_LIST)
                .one())
            .containsExactly(testUuids);
    }

    @Test
    public void testUuidArrayList() {
        assertThat(
            h2Extension.getSharedHandle()
                .createUpdate(U_INSERT)
                .bindArray("u", UUID.class, new ArrayList<>(Arrays.asList(testUuids)))
                .execute())
            .isOne();
        assertThat(
            h2Extension.getSharedHandle()
                .createQuery(U_SELECT)
                .mapTo(UUID_ARRAYLIST)
                .one())
            .containsExactly(testUuids);
    }

    @Test
    public void testUuidHashSet() {
        assertThat(
            h2Extension.getSharedHandle()
                .createUpdate(U_INSERT)
                .bindByType("u", new HashSet<>(Arrays.asList(testUuids)), UUID_SET)
                .execute())
            .isOne();
        assertThat(
            h2Extension.getSharedHandle()
                .createQuery(U_SELECT)
                .mapTo(UUID_HASHSET)
                .one())
            .containsExactlyInAnyOrder(testUuids);
    }

    @Test
    public void testUuidLinkedHashSet() {
        assertThat(
            h2Extension.getSharedHandle()
                .createUpdate(U_INSERT)
                .bindByType("u", new LinkedHashSet<>(Arrays.asList(testUuids)), UUID_SET)
                .execute())
            .isOne();
        assertThat(
            h2Extension.getSharedHandle()
                .createQuery(U_SELECT)
                .mapTo(UUID_LINKEDHASHSET)
                .one())
            .isInstanceOf(LinkedHashSet.class)
            .containsExactly(testUuids);
    }

    @Test
    public void testEnumArrays() {
        GenericType<List<TestEnum>> testEnumList = new GenericType<List<TestEnum>>() {};

        assertThat(h2Extension.getSharedHandle()
            .select("select ?")
            .bindByType(0, Arrays.asList(TestEnum.values()), testEnumList)
            .mapTo(testEnumList).one())
            .containsExactly(TestEnum.values());
    }

    public enum TestEnum {
        FOO, BAR, BAZ
    }
}
