package org.jdbi.v3.wip.categories;

import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;

import static org.jdbi.v3.wip.categories.PluginUtils.registerClassArgument;
import static org.jdbi.v3.wip.categories.PluginUtils.registerClassMapper;

@Deprecated
public class UnreliableJavaTimePlugin implements JdbiPlugin {
	@Override
	public void customizeJdbi(Jdbi jdbi) {
		registerClassArgument(jdbi, LocalDate.class, Types.DATE, (p, i, v) -> p.setDate(i, java.sql.Date.valueOf(v)));
		registerClassArgument(jdbi, LocalTime.class, Types.TIME, (p, i, v) -> p.setTime(i, Time.valueOf(v)));
		registerClassArgument(jdbi, LocalDateTime.class, Types.TIMESTAMP, (p, i, v) -> p.setTimestamp(i, Timestamp.valueOf(v)));

		registerClassArgument(jdbi, Instant.class, Types.TIMESTAMP, (p, i, v) -> p.setTimestamp(i, Timestamp.from(v)));
		registerClassArgument(jdbi, OffsetDateTime.class, Types.TIMESTAMP, (p, i, v) -> p.setTimestamp(i, Timestamp.from(v.toInstant())));
		registerClassArgument(jdbi, ZonedDateTime.class, Types.TIMESTAMP, (p, i, v) -> p.setTimestamp(i, Timestamp.from(v.toInstant())));
		registerClassArgument(jdbi, Year.class, Types.INTEGER, (p, i, v) -> p.setInt(i, v.getValue()));

		registerClassMapper(jdbi, LocalDate.class, (r, i) -> r.getDate(i).toLocalDate());
		registerClassMapper(jdbi, LocalTime.class, (r, i) -> r.getTime(i).toLocalTime());
		registerClassMapper(jdbi, LocalDateTime.class, (r, i) -> r.getTimestamp(i).toLocalDateTime());

		registerClassMapper(jdbi, Instant.class, (r, i) -> r.getTimestamp(i).toInstant());
		registerClassMapper(jdbi, OffsetDateTime.class, (r, i) -> OffsetDateTime.ofInstant(r.getTimestamp(i).toInstant(), ZoneId.systemDefault()));
		registerClassMapper(jdbi, ZonedDateTime.class, (r, i) -> r.getTimestamp(i).toInstant().atZone(ZoneId.systemDefault()));
		registerClassMapper(jdbi, Year.class, (r, i) -> Year.of(r.getInt(i)));
	}
}
