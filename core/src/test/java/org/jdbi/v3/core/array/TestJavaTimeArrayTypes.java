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
package org.jdbi.v3.core.array;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Map;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Runs the {@code java.time} array battery against H2, covering the default registrations in
 * {@link SqlArrayTypes} with no database plugin installed.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestJavaTimeArrayTypes extends AbstractJavaTimeArrayTests {

    private static final Map<Class<?>, String> H2_ARRAY_TESTS = Map.of(
        LocalDate.class, "d",
        LocalTime.class, "t",
        LocalDateTime.class, "ts",
        OffsetDateTime.class, "tstz",
        OffsetTime.class, "ttz",
        Instant.class, "tstz",
        ZonedDateTime.class, "tstz");

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    TestJavaTimeArrayTypes() {
        super(H2_ARRAY_TESTS);
    }

    @BeforeEach
    public void setUp() {
        try (var handle = h2Extension.openHandle()) {
            handle.useTransaction(th -> {
                th.execute("drop table if exists time_array_test");
                th.execute("""
                        create table time_array_test (
                        d date array,
                        t time(6) array,
                        ts timestamp array,
                        tstz timestamp with time zone array,
                        ttz time(6) with time zone array)
                    """);
            });
        }
    }

    @Override
    protected Handle getHandle() {
        return h2Extension.openHandle();
    }
}
