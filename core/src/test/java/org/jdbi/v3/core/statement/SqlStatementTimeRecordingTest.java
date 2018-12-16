package org.jdbi.v3.core.statement;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
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
    private static final Instant T0 = LocalDate.of(2018, Month.JANUARY, 1).atTime(LocalTime.of(12, 0, 0)).atZone(ZoneOffset.UTC).toInstant();
    private static final Instant
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
        when(clock.instant()).thenReturn(T0, T1, T2, T3);
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

                assertThat(logger.before).isEqualTo(T0);
                assertThat(logger.after).isEqualTo(T1);
                assertThat(logger.exception).isNull();

                logger.before = null;
                logger.after = null;

                assertThatThrownBy(h.createUpdate("insert into foo(bar) values (null)")::execute)
                    .isInstanceOf(UnableToExecuteStatementException.class);

                verify(clock, times(4)).instant();

                assertThat(logger.before).isEqualTo(T2);
                assertThat(logger.after).isNull();
                assertThat(logger.exception).isEqualTo(T3);
            });
    }

    private static class TimerSqlLogger implements SqlLogger {
        private Instant before, after, exception;

        @Override
        public void logBeforeExecution(StatementContext context) {
            before = context.getExecutionMoment();
        }

        @Override
        public void logAfterExecution(StatementContext context) {
            after = context.getCompletionMoment();
        }

        @Override
        public void logException(StatementContext context, SQLException ex) {
            exception = context.getExceptionMoment();
        }
    }
}
