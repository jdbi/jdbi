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
package org.jdbi.v3.sqlobject.config;

import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.SqlObjects;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.ParameterCustomizerFactory;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestUseConfiguredDefaultParameterCustomizerFactory {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    private Handle handle;

    private AtomicInteger invocationCounter = new AtomicInteger(0);

    @BeforeEach
    public void setUp() {
        Jdbi db = h2Extension.getJdbi();

        ParameterCustomizerFactory defaultParameterCustomizerFactory = (sqlObjectType, method, param, index, type) -> {
            invocationCounter.incrementAndGet();
            return (stmt, arg) -> stmt.bind("mybind" + index, arg);
        };

        db.configure(SqlObjects.class, c -> c.setDefaultParameterCustomizerFactory(defaultParameterCustomizerFactory));
        handle = db.open();
    }

    @AfterEach
    public void tearDown() {
        handle.close();
    }

    @Test
    public void shouldUseConfiguredSqlParameterCustomizer() {
        SomethingDao h = handle.attach(SomethingDao.class);
        h.findByNameAndIdNoBindAnnotation(1, "Joy");

        // factory is called twice for each parameters, once in
        // warm() and once in apply()
        assertThat(invocationCounter.get()).isEqualTo(4);
    }

    @Test
    public void shouldUseSqlParameterCustomizerFromAnnotation() {
        SomethingDao h = handle.attach(SomethingDao.class);
        h.findByNameAndIdWithBindAnnotation(1, "Joy");

        // called twice for each parameter
        assertThat(invocationCounter.get()).isEqualTo(2);
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface SomethingDao {

        @SqlQuery("select id, name from something where name = :mybind1 and id = :mybind0")
        Something findByNameAndIdNoBindAnnotation(int id, String name);

        @SqlQuery("select id, name from something where name = :mybind1 and id = :mybind0")
        Something findByNameAndIdWithBindAnnotation(@Bind("mybind0") int id, @Bind("mybind1") String name);

    }

}
