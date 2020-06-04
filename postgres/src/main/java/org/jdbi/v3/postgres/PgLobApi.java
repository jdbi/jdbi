package org.jdbi.v3.postgres;

import java.io.InputStream;

import org.jdbi.v3.meta.Beta;

@Beta
public interface PgLobApi {
    long createLob();
    void deleteLob(long oid);
    void writeLob(long oid, InputStream data);
    InputStream readLob(long oid);
}
