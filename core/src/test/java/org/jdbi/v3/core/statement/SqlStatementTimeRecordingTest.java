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

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.jdbi.v3.core.Time;
import org.jdbi.v3.core.rule.DatabaseRule;
import org.jdbi.v3.core.rule.SqliteDatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SqlStatementTimeRecordingTest {
    private static final ZonedDateTime T0 = LocalDate.of(2018, Month.JANUARY, 1).atTime(LocalTime.of(12, 0, 0)).atZone(ZoneOffset.UTC);
    private static final ZonedDateTime
        T1 = T0.plusSeconds(10),
        T2 = T0.plusSeconds(20),
        T3 = T0.plusSeconds(30);

    @Rule
    public DatabaseRule db = new SqliteDatabaseRule();
    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();
    @Mock
    private Clock clock;

    @Before
    public void before() {
        when(clock.instant()).thenReturn(T0.toInstant(), T1.toInstant(), T2.toInstant(), T3.toInstant());
        when(clock.getZone()).thenReturn(T0.getZone());
        db.getJdbi().getConfig(Time.class).setClock(clock);
    }

    /**
     * check {@link SqlLoggerUtil#wrap}
     */
    @Test
    public void ensureTimesAreRecorded() {
        TimerSqlLogger logger = new TimerSqlLogger();

        db.getJdbi()
            .configure(SqlStatements.class, sql -> sql.setSqlLogger(logger))
            .useHandle(h -> {
                h.createUpdate("create table foo(bar int primary key not null)").execute();

                verify(clock, times(2)).instant();
                verify(clock, times(2)).getZone();

                assertThat(logger.before).isEqualTo(T0.toInstant());
                assertThat(logger.beforeZdt).isEqualTo(T0);
                assertThat(logger.after).isEqualTo(T1.toInstant());
                assertThat(logger.afterZdt).isEqualTo(T1);
                assertThat(logger.exception).isNull();
                assertThat(logger.exceptionZdt).isNull();

                logger.before = null;
                logger.beforeZdt = null;
                logger.after = null;
                logger.afterZdt = null;

                assertThatThrownBy(h.createUpdate("insert into foo(bar) values (null)")::execute)
                    .isInstanceOf(UnableToExecuteStatementException.class);

                verify(clock, times(4)).instant();

                assertThat(logger.before).isEqualTo(T2.toInstant());
                assertThat(logger.beforeZdt).isEqualTo(T2);
                assertThat(logger.after).isNull();
                assertThat(logger.afterZdt).isNull();
                assertThat(logger.exception).isEqualTo(T3.toInstant());
                assertThat(logger.exceptionZdt).isEqualTo(T3);
            });
    }

    private static class TimerSqlLogger implements SqlLogger {
        private Instant before, after, exception;
        private ZonedDateTime beforeZdt, afterZdt, exceptionZdt;

        @Override
        public void logBeforeExecution(StatementContext context) {
            before = context.getExecutionMoment();
            beforeZdt = context.getExecutionTimestamp();
        }

        @Override
        public void logAfterExecution(StatementContext context) {
            after = context.getCompletionMoment();
            afterZdt = context.getCompletionTimestamp();
            exception = context.getExceptionMoment();
            exceptionZdt = context.getExceptionTimestamp();
        }

        @Override
        public void logException(StatementContext context, SQLException ex) {
            after = context.getCompletionMoment();
            afterZdt = context.getCompletionTimestamp();
            exception = context.getExceptionMoment();
            exceptionZdt = context.getExceptionTimestamp();
        }
    }
}
