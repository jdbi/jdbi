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
package org.jdbi.core.statement;

import java.util.UUID;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.core.Handle;
import org.jdbi.core.junit5.PgDatabaseExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestQueriesPG {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public PgDatabaseExtension pgExtension = PgDatabaseExtension.instance(pg)
        .withInitializer(h -> h.execute("create table something (id integer primary key, uuid UUID)"));

    @Test
    @DisplayName("HashPrefixSqlParser works with colon-based casting in postgresql (#1389)")
    public void testCastingWithHashParser() {
        final Handle h = pgExtension.getSharedHandle()
            .setSqlParser(new HashPrefixSqlParser());

        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        h.createUpdate("insert into something (id, uuid) values (1, #uuid)").bind("uuid", uuid1).execute();
        h.createUpdate("insert into something (id, uuid) values (2, #uuid)").bind("uuid", uuid2).execute();

        final int result = h.createQuery("select id from something WHERE uuid = #value::UUID")
            .bind("value", uuid2.toString())
            .mapTo(Integer.class)
            .one();
        assertThat(result).isEqualTo(2);
    }
}
