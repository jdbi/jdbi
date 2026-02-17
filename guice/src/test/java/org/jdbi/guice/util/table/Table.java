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
package org.jdbi.guice.util.table;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.mapper.ColumnMapper;
import org.jdbi.core.mapper.ColumnMappers;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.statement.StatementContext;
import org.jdbi.guice.util.JsonCodec;

public final class Table {

    private final UUID uuid;
    private final String s;
    private final ImmutableMap<String, Object> j;

    public Table(UUID uuid, String s, ImmutableMap<String, Object> j) {
        this.uuid = uuid;
        this.s = s;
        this.j = j;
    }

    public static Table randomTable() {
        return new Table(UUID.randomUUID(),
            UUID.randomUUID().toString(),
            ImmutableMap.of(
                "data", UUID.randomUUID().toString(),
                "flag", ThreadLocalRandom.current().nextBoolean()));
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getS() {
        return s;
    }

    public ImmutableMap<String, Object> getJ() {
        return j;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Table table = (Table) o;
        return Objects.equals(uuid, table.uuid) && Objects.equals(s, table.s) && Objects.equals(j, table.j);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, s, j);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("uuid", uuid)
            .add("s", s)
            .add("j", j)
            .toString();
    }

    @Singleton
    public static class TableMapper implements RowMapper<Table> {

        private ColumnMapper<ImmutableMap<String, Object>> jsonMapper;

        @Inject
        public TableMapper() {}

        @Override
        public void init(ConfigRegistry registry) {
            this.jsonMapper = registry.get(ColumnMappers.class).findFor(JsonCodec.TYPE).orElseThrow(IllegalStateException::new);
        }

        @Override
        public Table map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Table(
                rs.getObject("u", UUID.class),
                rs.getString("s"),
                jsonMapper.map(rs, "j", ctx));
        }
    }
}
