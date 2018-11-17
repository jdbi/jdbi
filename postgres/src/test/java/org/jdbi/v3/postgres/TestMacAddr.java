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

import static org.assertj.core.api.Assertions.assertThat;

import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestMacAddr {
    @Rule
    public JdbiRule db = PostgresDbRule.rule();

    @Before
    public void setUp() {
        db.getJdbi().useHandle(handle -> handle.execute("create table macaddrs (id int, address macaddr)"));
    }

    @Test
    public void macAddr() {
        MacAddrDao dao = db.getJdbi().onDemand(MacAddrDao.class);

        dao.insert(1, "deadbeef1234");
        assertThat(dao.select(1)).isEqualTo("de:ad:be:ef:12:34");

        dao.insert(2, "1234567890ab");
        assertThat(dao.select(2)).isEqualTo("12:34:56:78:90:ab");
    }

    public interface MacAddrDao {
        @SqlUpdate("insert into macaddrs (id, address) values (:id, :address)")
        void insert(int id,
                    @MacAddr String address);

        @SqlQuery("select address from macaddrs where id = :id")
        @MacAddr
        String select(int id);
    }
}
