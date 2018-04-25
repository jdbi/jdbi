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
package org.jdbi.v3.core.array;

import java.util.UUID;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestVendorArrays {

    private void init(Jdbi db) {
        db.registerArrayType(Integer.class, "int");
        db.registerArrayType(String.class, "varchar");
    }

    @Test
    public void testHsqlDb() {
        Jdbi db = Jdbi.create("jdbc:hsqldb:mem:" + UUID.randomUUID());
        init(db);

        try (Handle handle = db.open()) {
            handle.execute("create table player_stats ("
                + "name varchar(64) primary key, "
                + "seasons varchar(36) array, "
                + "points int array)");
            handle.createUpdate("insert into player_stats (name,seasons,points) values (?,?,?)")
                    .bind(0, "Jack Johnson")
                    .bind(1, new String[]{"2013-2014", "2014-2015", "2015-2016"})
                    .bind(2, new Integer[]{42, 51, 50})
                    .execute();

            String[] seasons = handle.createQuery("select seasons from player_stats where name=:name")
                    .bind("name", "Jack Johnson")
                    .mapTo(String[].class)
                    .findOnly();
            assertThat(seasons).containsExactly("2013-2014", "2014-2015", "2015-2016");
        }
    }
}
