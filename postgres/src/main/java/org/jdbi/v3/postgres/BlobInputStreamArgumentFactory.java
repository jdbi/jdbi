package org.jdbi.v3.postgres;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.StatementContext;

class BlobInputStreamArgumentFactory extends AbstractArgumentFactory<InputStream> {
    BlobInputStreamArgumentFactory() {
        super(Types.BLOB);
    }

    @Override
    protected Argument build(InputStream value, ConfigRegistry config) {
        return new LobInputStreamArgument(value);
    }

    static class LobInputStreamArgument implements Argument {
        private final InputStream value;

        LobInputStreamArgument(InputStream value) {
            this.value = value;
        }

        @Override
        public void apply(int pos, PreparedStatement stmt, StatementContext ctx) throws SQLException {
            PgLobApi lob = ctx.getConfig(PostgresTypes.class).getLobApi();
            long oid = lob.createLob();
            lob.writeLob(oid, value);
            stmt.setLong(pos, oid);
        }
    }
}
