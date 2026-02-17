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
package org.jdbi.postgres;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.jdbi.core.argument.AbstractArgumentFactory;
import org.jdbi.core.argument.Argument;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.statement.StatementContext;

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
