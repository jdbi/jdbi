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
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementParameterCustomizer;
import org.jdbi.v3.sqlobject.statement.SqlObjectStatementCustomizerConfiguration;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSqlObjectStatementCustomizer
{
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    private SqlStatementCustomizerFactory sqlStatementParameterCustomizer;

    private AtomicInteger invocationCounter = new AtomicInteger(0);

    @Before
    public void setUp() throws Exception
    {
        Jdbi db = dbRule.getJdbi();

        sqlStatementParameterCustomizer = new SqlStatementCustomizerFactory()
        {
            SqlStatementCustomizerFactory defaultFactory = new Bind.Factory();

            @Override
            public SqlStatementParameterCustomizer createForParameter(Annotation annotation, Class<?> sqlObjectType, Method method, Parameter param, int index)
            {
                invocationCounter.incrementAndGet();
                return defaultFactory.createForParameter(annotation, sqlObjectType, method, param, index);
            }
        };


        db.configure(SqlObjectStatementCustomizerConfiguration.class, c -> c.setDefaultParameterCustomizerFactory(sqlStatementParameterCustomizer));
        handle = db.open();
    }

    @Test
    public void shouldUseConfiguredSqlParameterCustomizer() throws Exception
    {
        SomethingDao h = handle.attach(SomethingDao.class);
        h.findByIdNoBindAnnotation(1);

        assertThat(invocationCounter.get()).isEqualTo(1);

    }

    @Test
    public void shouldUseSqlParameterCustomizerFromAnnotation() throws Exception
    {
        SomethingDao h = handle.attach(SomethingDao.class);
        h.findByIdWithBindAnnotation(1);

        assertThat(invocationCounter.get()).isEqualTo(0);

    }


    @RegisterRowMapper(SomethingMapper.class)
    public interface SomethingDao
    {
        @SqlQuery("select id, name from something where id = :id")
        Something findByIdNoBindAnnotation(int id);

        @SqlQuery("select id, name from something where id = :id")
        Something findByIdWithBindAnnotation(@Bind("id") int id);

    }

}
