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
package org.jdbi.core.argument;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Set;
import java.util.stream.Collectors;

import org.jdbi.core.Handle;
import org.jdbi.core.Sql;
import org.jdbi.core.internal.testing.DatabaseExtension;
import org.jdbi.core.internal.testing.H2DatabaseExtension;
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
    public void testInetAddress2() throws Exception {
        try (Handle h = dbExtension.getJdbi().open()) {
            h.execute("CREATE TABLE inet_test (ipaddress " + getInetType() + ")");

            InetAddress inetAddrIn = InetAddress.getByName("8.8.8.8");
            assertThat(inetAddrIn).isInstanceOf(Inet4Address.class);

            // insert IP address into column of type INET
            int update1Count = h.createUpdate(Sql.of("INSERT INTO inet_test (ipaddress) VALUES (:address)"))
                .bind("address", inetAddrIn)
                .execute();
            assertThat(update1Count).isOne();

            // read back the inserted IP address
            InetAddress inetAddr1Out = h.createQuery(Sql.of("SELECT ipaddress FROM inet_test"))
                .mapTo(InetAddress.class)
                .one();

            // test for equality
            assertThat(inetAddrIn).isEqualTo(inetAddr1Out);

            // update the record to a null IP address
            int update2Count = h.createUpdate(Sql.of("UPDATE inet_test SET ipaddress = :address"))
                .bind("address", (InetAddress) null)
                .execute();
            assertThat(update2Count).isOne();

            // read back the record ensuring proper null handling
            InetAddress inetAddr2Out = h.createQuery(Sql.of("SELECT ipaddress FROM inet_test"))
                .mapTo(InetAddress.class)
                .one();
            assertThat(inetAddr2Out).isNull();
        }
    }

    protected String getInetType() {
        return "VARCHAR";
    }

}
