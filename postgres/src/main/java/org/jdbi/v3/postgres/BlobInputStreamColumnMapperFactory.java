package org.jdbi.v3.postgres;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.core.statement.StatementContext;

class BlobInputStreamColumnMapperFactory implements ColumnMapperFactory {
    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        if (InputStream.class != type) {
            return Optional.empty();
        }
        return Optional.of(new LobColumnMapper());
    }
    static class LobColumnMapper implements ColumnMapper<InputStream> {
        @Override
        public InputStream map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            return ctx.getConfig(PostgresTypes.class)
                    .getLobApi()
                    .readLob(r.getLong(columnNumber));
        }
    }
}
