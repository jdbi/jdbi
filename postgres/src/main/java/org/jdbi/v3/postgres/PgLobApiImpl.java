/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
