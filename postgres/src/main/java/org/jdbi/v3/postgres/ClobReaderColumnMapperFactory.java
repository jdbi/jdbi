package org.jdbi.v3.postgres;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.postgres.BlobInputStreamColumnMapperFactory.LobColumnMapper;

class ClobReaderColumnMapperFactory implements ColumnMapperFactory {
    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        if (Reader.class != type) {
            return Optional.empty();
        }
        LobColumnMapper inner = new LobColumnMapper();
        return Optional.of((rs, col, ctx) -> new InputStreamReader(inner.map(rs, col, ctx), StandardCharsets.UTF_8));
    }
}
