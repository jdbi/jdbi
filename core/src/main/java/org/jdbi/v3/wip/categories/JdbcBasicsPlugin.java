package org.jdbi.v3.wip.categories;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;

import static org.jdbi.v3.wip.categories.PluginUtils.registerClassArgument;
import static org.jdbi.v3.wip.categories.PluginUtils.registerClassMapper;

public class JdbcBasicsPlugin implements JdbiPlugin {
	@Override
	public void customizeJdbi(Jdbi jdbi) {
		registerClassArgument(jdbi, BigDecimal.class, Types.NUMERIC, PreparedStatement::setBigDecimal);
		registerClassArgument(jdbi, String.class, Types.VARCHAR, PreparedStatement::setString);
		registerClassArgument(jdbi, URL.class, Types.DATALINK, PreparedStatement::setURL);
		registerClassArgument(jdbi, byte[].class, Types.VARBINARY, PreparedStatement::setBytes);
		registerClassArgument(jdbi, Blob.class, Types.BLOB, PreparedStatement::setBlob);
		registerClassArgument(jdbi, Clob.class, Types.CLOB, PreparedStatement::setClob);

		registerClassMapper(jdbi, BigDecimal.class, ResultSet::getBigDecimal);
		registerClassMapper(jdbi, String.class, ResultSet::getString);
		registerClassMapper(jdbi, URL.class, ResultSet::getURL);
		registerClassMapper(jdbi, byte[].class, ResultSet::getBytes);
		registerClassMapper(jdbi, Blob.class, ResultSet::getBlob);
		registerClassMapper(jdbi, Clob.class, ResultSet::getClob);
	}
}
