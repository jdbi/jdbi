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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.SqlObjects;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.ParameterCustomizerFactory;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class TestUseConfiguredDefaultParameterCustomizerFactory {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    private AtomicInteger invocationCounter = new AtomicInteger(0);

    @Before
    public void setUp() throws Exception {
        Jdbi db = dbRule.getJdbi();

        ParameterCustomizerFactory defaultParameterCustomizerFactory = (sqlObjectType, method, param, index, type) -> {
            invocationCounter.incrementAndGet();
            return (stmt, arg) -> stmt.bind("mybind" + index, arg);
        };

        db.configure(SqlObjects.class, c -> c.setDefaultParameterCustomizerFactory(defaultParameterCustomizerFactory));
        handle = db.open();
    }

    @Test
    public void shouldUseConfiguredSqlParameterCustomizer() {
        SomethingDao h = handle.attach(SomethingDao.class);
        h.findByNameAndIdNoBindAnnotation(1, "Joy");

        assertThat(invocationCounter.get()).isEqualTo(2);
    }

    @Test
    public void shouldUseSqlParameterCustomizerFromAnnotation() {
        SomethingDao h = handle.attach(SomethingDao.class);
        h.findByNameAndIdWithBindAnnotation(1, "Joy");

        assertThat(invocationCounter.get()).isEqualTo(0);
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface SomethingDao {

        @SqlQuery("select id, name from something where name = :mybind1 and id = :mybind0")
        Something findByNameAndIdNoBindAnnotation(int id, String name);

        @SqlQuery("select id, name from something where name = :mybind1 and id = :mybind0")
        Something findByNameAndIdWithBindAnnotation(@Bind("mybind0") int id, @Bind("mybind1") String name);

    }

}
