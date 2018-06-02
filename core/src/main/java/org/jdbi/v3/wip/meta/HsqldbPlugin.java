package org.jdbi.v3.wip.meta;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.wip.GetSetObjectPlugin;

public class HsqldbPlugin implements JdbiPlugin {
	@Override
	public void customizeJdbi(Jdbi jdbi) {
		jdbi.installPlugin(EssentialsPlugin.CORE);
		jdbi.installPlugin(new GetSetObjectPlugin(
			UUID.class,
			LocalDate.class,
			LocalTime.class,
			LocalDateTime.class,
			OffsetTime.class,
			OffsetDateTime.class
		));
	}
}
