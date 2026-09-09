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
package org.jdbi.v3.sqlobject;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins that configuration added after an extension's first use is observed by later uses: the Jdbi
 * root configuration for on-demand extensions, and a handle's local configuration for repeated
 * {@link Handle#attach(Class)} calls on the same handle.
 */
public class TestReconfigureAfterAttach {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    @Test
    public void onDemandObservesMapperRegisteredAfterFirstUse() {
        Jdbi jdbi = h2Extension.getJdbi();
        Dao dao = jdbi.onDemand(Dao.class);

        dao.insert(1, "brian");
        jdbi.registerRowMapper(new SomethingMapper());

        assertThat(dao.list()).extracting(Something::getName).containsExactly("brian");
    }

    @Test
    public void repeatedAttachObservesHandleLocalRegistration() {
        try (Handle handle = h2Extension.getJdbi().open()) {
            handle.attach(Dao.class).insert(1, "brian");
            handle.registerRowMapper(new SomethingMapper());

            assertThat(handle.attach(Dao.class).list()).extracting(Something::getName).containsExactly("brian");
        }
    }

    public interface Dao {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select id, name from something")
        List<Something> list();
    }
}
