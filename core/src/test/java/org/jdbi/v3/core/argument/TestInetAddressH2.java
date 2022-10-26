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
package org.jdbi.v3.core.argument;

import java.net.InetAddress;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.junit5.DatabaseExtension;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestInetAddressH2 {

    @RegisterExtension
    public DatabaseExtension<?> dbExtension = H2DatabaseExtension.instance();

    @Test
    public void testInetAddress() throws Exception {
        dbExtension.getJdbi().useHandle(h -> {
            h.execute("CREATE TABLE addrs (addr " + getInetType() + " PRIMARY KEY)");

            String insert = "INSERT INTO addrs VALUES(?)";
            InetAddress ipv4 = InetAddress.getByName("1.2.3.4");
            InetAddress ipv6 = InetAddress.getByName("fe80::226:8ff:fefa:d1e3");

            h.createUpdate(insert)
                .bind(0, ipv4)
                .execute();

            h.createUpdate(insert)
                .bind(0, ipv6)
                .execute();

            Set<InetAddress> addrs = h.createQuery("SELECT * FROM addrs")
                .mapTo(InetAddress.class)
                .collect(Collectors.toSet());
            assertThat(addrs).containsOnly(ipv4, ipv6);
        });
    }

    @Test
    public void testInetAddressColumn() throws Exception {
        dbExtension.getJdbi().useHandle(h -> {
            h.execute("CREATE TABLE addrs (name VARCHAR PRIMARY KEY, addr " + getInetType() + " )");
            h.registerRowMapper(new AddrRowMapper());

            String insert = "INSERT INTO addrs VALUES(?, ?)";
            InetAddress ipv4 = InetAddress.getByName("1.2.3.4");
            InetAddress ipv6 = InetAddress.getByName("fe80::226:8ff:fefa:d1e3");

            h.createUpdate(insert)
                .bind(0, "ipv4-host")
                .bind(1, ipv4)
                .execute();

            h.createUpdate(insert)
                .bind(0, "ipv6-host")
                .bind(1, ipv6)
                .execute();

            Set<AddrRow> addrs = h.createQuery("SELECT * FROM addrs")
                .mapTo(AddrRow.class)
                .collect(Collectors.toSet());
            assertThat(addrs).hasSize(2);
        });
    }

    public static class AddrRow {

        private final String name;
        private final InetAddress addr;

        public AddrRow(String name, InetAddress addr) {
            this.name = name;
            this.addr = addr;
        }

        public String getName() {
            return name;
        }

        public InetAddress getAddr() {
            return addr;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AddrRow addrRow = (AddrRow) o;
            return Objects.equals(name, addrRow.name) && Objects.equals(addr, addrRow.addr);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, addr);
        }
    }

    public static class AddrRowMapper implements RowMapper<AddrRow> {

        private ColumnMapper<InetAddress> inetAddressColumnMapper;

        @Override
        public AddrRow map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new AddrRow(rs.getString("name"), inetAddressColumnMapper.map(rs, "addr", ctx));
        }

        @Override
        public void init(ConfigRegistry registry) {
            inetAddressColumnMapper = registry.get(ColumnMappers.class).findFor(InetAddress.class).orElseThrow(IllegalArgumentException::new);
        }
    }

    protected String getInetType() {
        return "VARCHAR";
    }
}
