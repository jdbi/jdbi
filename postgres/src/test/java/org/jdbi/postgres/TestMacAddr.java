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

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestMacAddr {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg).withPlugins(new SqlObjectPlugin(), new PostgresPlugin())
        .withInitializer((ds, h) -> h.execute("create table macaddrs (id int, address macaddr)"));

    @Test
    public void macAddr() {
        MacAddrDao dao = pgExtension.attach(MacAddrDao.class);

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
