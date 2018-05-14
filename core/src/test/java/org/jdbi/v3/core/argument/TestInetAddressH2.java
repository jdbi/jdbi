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

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.util.Set;
import java.util.stream.Collectors;

import org.jdbi.v3.core.rule.DatabaseRule;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Rule;
import org.junit.Test;

public class TestInetAddressH2 {
    @Rule
    public DatabaseRule dbRule = new H2DatabaseRule();

    @Test
    public void testInetAddress() throws Exception {
        dbRule.getJdbi().useHandle(h -> {
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

    protected String getInetType() {
        return "VARCHAR";
    }
}
