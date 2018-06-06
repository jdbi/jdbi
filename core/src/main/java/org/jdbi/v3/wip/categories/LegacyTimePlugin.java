package org.jdbi.v3.wip.categories;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;

import static org.jdbi.v3.wip.categories.PluginUtils.registerClassArgument;
import static org.jdbi.v3.wip.categories.PluginUtils.registerClassMapper;

@Deprecated
public class LegacyTimePlugin implements JdbiPlugin {
	@Override
	public void customizeJdbi(Jdbi jdbi) {
		registerClassArgument(jdbi, Date.class, Types.DATE, PreparedStatement::setDate);
		registerClassArgument(jdbi, Time.class, Types.TIME, PreparedStatement::setTime);
		registerClassArgument(jdbi, Timestamp.class, Types.TIMESTAMP, PreparedStatement::setTimestamp);

		registerClassMapper(jdbi, Date.class, ResultSet::getDate);
		registerClassMapper(jdbi, Time.class, ResultSet::getTime);
		registerClassMapper(jdbi, Timestamp.class, ResultSet::getTimestamp);
	}
}
