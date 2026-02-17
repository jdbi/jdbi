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
package org.jdbi.core.mapper.reflect;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.core.Handle;
import org.jdbi.core.junit5.PgDatabaseExtension;
import org.jdbi.core.mapper.reflect.ConstructorMapperTest.ConstructorBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class ConstructorMapperPgTest {
    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();
    @RegisterExtension
    public PgDatabaseExtension pgExtension = PgDatabaseExtension.instance(pg);

    private Handle handle;

    @BeforeEach
    public void setUp() {
        handle = pgExtension.getSharedHandle()
            .registerRowMapper(ConstructorMapper.factory(ConstructorBean.class));

        handle.execute("CREATE TABLE bean (s varchar primary key, i integer)");
    }

    @Test
    public void testEmptyGeneratedKeys() {
        handle.execute("INSERT INTO bean VALUES('3', 2)");
        assertThat(handle
                .prepareBatch("INSERT INTO bean(s, i) VALUES(:s, :i) ON CONFLICT (s) DO NOTHING")
                .bind("s", "3")
                .bind("i", 2)
                .executePreparedBatch("s", "i")
                .mapTo(ConstructorBean.class)
                .list())
            .isEmpty();
    }
}
