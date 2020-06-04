package org.jdbi.v3.postgres;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;

import org.postgresql.PGConnection;
import org.postgresql.largeobject.LargeObject;
import org.postgresql.largeobject.LargeObjectManager;

class PgLobApiImpl implements PgLobApi {
    private static final int BUF_SIZE = 1024 * 4;
    private final LargeObjectManager mgr;

    PgLobApiImpl(Connection conn) {
        try {
            this.mgr = conn.unwrap(PGConnection.class)
                    .getLargeObjectAPI();
        } catch (SQLException e) {
            throw new LargeObjectException(e);
        }
    }

    @Override
    public long createLob() {
        try {
            return mgr.createLO();
        } catch (SQLException e) {
            throw new LargeObjectException(e);
        }
    }

    @Override
    @SuppressWarnings("PMD.AssignmentInOperand")
    public void writeLob(long oid, InputStream data) {
        try (LargeObject lob = mgr.open(oid)) {
            byte[] buf = new byte[BUF_SIZE];
            int read;
            while ((read = data.read(buf, 0, buf.length)) > -1) {
                if (read > 0) {
                    lob.write(buf, 0, read);
                }
            }
        } catch (SQLException | IOException e) {
            throw new LargeObjectException(e);
        }
    }

    @Override
    public InputStream readLob(long oid) {
        try {
            return mgr.open(oid, LargeObjectManager.READ).getInputStream();
        } catch (SQLException e) {
            throw new LargeObjectException(e);
        }
    }

    @Override
    public void deleteLob(long oid) {
        try {
            mgr.delete(oid);
        } catch (SQLException e) {
            throw new LargeObjectException(e);
        }
    }
}
