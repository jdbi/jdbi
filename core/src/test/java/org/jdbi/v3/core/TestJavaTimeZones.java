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
package org.jdbi.v3.core;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.statement.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class TestJavaTimeZones {
    static final ZoneOffset TEST_ZONE_OFFSET;
    static final ZoneId TEST_ZONE_ID;

    static {
        var testZoneOffset = ZoneOffset.of("-02:30");
        if (ZoneId.systemDefault().getRules().getOffset(Instant.now()).equals(testZoneOffset)) {
            testZoneOffset = ZoneOffset.of("-09:30");
        }

        var testZoneId = ZoneId.of("Asia/Katmandu"); // +05:45 ...
        if (ZoneId.systemDefault().getId().equals(testZoneId.getId())) {
            testZoneId = ZoneId.of("Australia/Broken_Hill"); // +10:30 ...
        }

        TEST_ZONE_OFFSET = testZoneOffset;
        TEST_ZONE_ID = testZoneId;
    }

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    @BeforeEach
    public void setUp() {
        try (var h = h2Extension.openHandle()) {
            h.useTransaction(th -> {
                th.execute("drop table if exists time_test");
                th.execute("""
                        create table time_test (
                            z varchar
                        )
                    """);
            });
        }
    }

    @Test
    public final void testZoneId() {
        var h = h2Extension.getSharedHandle();

        final var expected = TEST_ZONE_ID;
        h.execute("insert into time_test(z) values (?)", expected);

        var result = h.createQuery("select z from time_test").mapTo(ZoneId.class).one();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    public final void testZoneOffset() {
        var h = h2Extension.getSharedHandle();

        final var expected = TEST_ZONE_OFFSET;
        h.execute("insert into time_test(z) values (?)", expected);

        var result = h.createQuery("select z from time_test").mapTo(ZoneOffset.class).one();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    public final void testZoneIdNull() {
        var h = h2Extension.getSharedHandle();

        try (Update u = h.createUpdate("insert into time_test(z) values (?)")) {
            u.bindByType(0, null, ZoneId.class);
            u.execute();
        }

        var result = h.createQuery("select z from time_test").mapTo(ZoneId.class).one();

        assertThat(result).isNull();
    }

    @Test
    public final void testZoneOffsetNull() {
        var h = h2Extension.getSharedHandle();

        try (Update u = h.createUpdate("insert into time_test(z) values (?)")) {
            u.bindByType(0, null, ZoneOffset.class);
            u.execute();
        }

        var result = h.createQuery("select z from time_test").mapTo(ZoneOffset.class).one();

        assertThat(result).isNull();
    }
}
