package org.jdbi.v3.postgres;

import java.io.Reader;
import java.sql.Types;

import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.postgres.BlobInputStreamArgumentFactory.LobInputStreamArgument;
import org.postgresql.util.ReaderInputStream;

class ClobReaderArgumentFactory extends AbstractArgumentFactory<Reader> {
    ClobReaderArgumentFactory() {
        super(Types.CLOB);
    }

    @Override
    protected Argument build(Reader value, ConfigRegistry config) {
        return new LobInputStreamArgument(new ReaderInputStream(value));
    }
}
