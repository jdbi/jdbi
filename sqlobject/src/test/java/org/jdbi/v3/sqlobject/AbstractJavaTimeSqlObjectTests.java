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
package org.jdbi.v3.sqlobject;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jdbi.v3.core.AbstractJavaTimeTests;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.meta.Legacy;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractJavaTimeSqlObjectTests extends AbstractJavaTimeTests {

    protected AbstractJavaTimeSqlObjectTests(Map<Class<?>, String[]> columnsForTypes) {
        super(columnsForTypes);
    }

    // a short, sharp regret that we do not support generic interfaces for DAOs. A separate interface is needed for any
    // type that is tested.

    public interface Dao<T> {
        @SqlUpdate("INSERT INTO time_test(<COLUMN>) VALUES (:value)")
        void insert(@Define("COLUMN") String column, @Bind("value") T value);

        @SingleValue
        @SqlQuery("SELECT <COLUMN> FROM time_test")
        T query(@Define("COLUMN") String column);
    }

    public interface LegacyDao<T> extends Dao<T> {
        @SqlUpdate("INSERT INTO time_test(<COLUMN>) VALUES (:value)")
        void insert(@Define("COLUMN") String column, @Bind("value") @Legacy T value);

        @SingleValue
        @SqlQuery("SELECT <COLUMN> FROM time_test")
        @Legacy
        T query(@Define("COLUMN") String column);
    }

    public interface TimeOffsetDateTimeDao extends Dao<OffsetDateTime> {}

    public interface TimeZonedDateTimeDao extends Dao<ZonedDateTime> {}

    public interface TimeOffsetTimeDao extends Dao<OffsetTime> {}

    public interface SqlTimeDao extends Dao<Time> {}

    public interface SqlDateDao extends Dao<Date> {}

    public interface SqlTimestampDao extends Dao<Timestamp> {}

    public interface TimeLocalTimeDao extends Dao<LocalTime> {}

    public interface TimeLocalDateDao extends Dao<LocalDate> {}

    public interface TimeLocalDateTimeDao extends Dao<LocalDateTime> {}

    public interface TimeInstantDao extends Dao<Instant> {}

    public interface LegacyTimeOffsetDateTimeDao extends LegacyDao<OffsetDateTime> {}

    public interface LegacyTimeZonedDateTimeDao extends LegacyDao<ZonedDateTime> {}

    public interface LegacyTimeOffsetTimeDao extends LegacyDao<OffsetTime> {}

    public interface LegacyTimeLocalTimeDao extends LegacyDao<LocalTime> {}

    public interface LegacyTimeLocalDateDao extends LegacyDao<LocalDate> {}

    public interface LegacyTimeLocalDateTimeDao extends LegacyDao<LocalDateTime> {}

    public interface LegacyTimeInstantDao extends LegacyDao<Instant> {}

    private static final Map<QualifiedType<?>, Class<? extends Dao<?>>> DAO_MAP =
        Map.ofEntries(
            Map.entry(QualifiedType.of(OffsetDateTime.class), TimeOffsetDateTimeDao.class),
            Map.entry(QualifiedType.of(ZonedDateTime.class), TimeZonedDateTimeDao.class),
            Map.entry(QualifiedType.of(OffsetTime.class), TimeOffsetTimeDao.class),
            Map.entry(QualifiedType.of(Time.class), SqlTimeDao.class),
            Map.entry(QualifiedType.of(Date.class), SqlDateDao.class),
            Map.entry(QualifiedType.of(Timestamp.class), SqlTimestampDao.class),
            Map.entry(QualifiedType.of(LocalTime.class), TimeLocalTimeDao.class),
            Map.entry(QualifiedType.of(LocalDate.class), TimeLocalDateDao.class),
            Map.entry(QualifiedType.of(LocalDateTime.class), TimeLocalDateTimeDao.class),
            Map.entry(QualifiedType.of(Instant.class), TimeInstantDao.class),
            Map.entry(QualifiedType.of(OffsetDateTime.class).with(Legacy.class), LegacyTimeOffsetDateTimeDao.class),
            Map.entry(QualifiedType.of(ZonedDateTime.class).with(Legacy.class), LegacyTimeZonedDateTimeDao.class),
            Map.entry(QualifiedType.of(OffsetTime.class).with(Legacy.class), LegacyTimeOffsetTimeDao.class),
            Map.entry(QualifiedType.of(LocalTime.class).with(Legacy.class), LegacyTimeLocalTimeDao.class),
            Map.entry(QualifiedType.of(LocalDate.class).with(Legacy.class), LegacyTimeLocalDateDao.class),
            Map.entry(QualifiedType.of(LocalDateTime.class).with(Legacy.class), LegacyTimeLocalDateTimeDao.class),
            Map.entry(QualifiedType.of(Instant.class).with(Legacy.class), LegacyTimeInstantDao.class)
        );

    @DisplayName("Test java.time types SQLObject roundtrip")
    @ParameterizedTest(name = "[{index}]: type = {0}, column = {1}")
    @MethodSource("argumentsProvider")
    public void testJavaTimeSqlObjectType(Class<?> clazz, String column, Supplier<Object> valueSupplier, Consumer<TestResult> comparator) {
        var type = getTestType(clazz);
        var value = valueSupplier.get();
        var expectedZoneOffset = getExpectedZoneOffset(clazz, value, column);

        @SuppressWarnings("unchecked")
        var dao = (Dao<Object>) h.attach(DAO_MAP.get(type));

        dao.insert(column, value);

        var result = dao.query(column);

        comparator.accept(new TestResult(value, result, column, clazz, expectedZoneOffset));
    }

    @DisplayName("Test java.time types SQLObject null value")
    @ParameterizedTest(name = "[{index}]: type = {0}, column = {1}")
    @MethodSource("argumentsProvider")
    public void testJavaTimeSqlObjectTypeNull(Class<?> clazz, String column) {
        var type = getTestType(clazz);

        var dao = h.attach(DAO_MAP.get(type));
        dao.insert(column, null);
        var result = dao.query(column);

        assertThat(result).isNull();
    }
}
