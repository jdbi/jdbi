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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.GenericMapMapperFactory;
import org.jdbi.v3.core.rule.SqliteDatabaseRule;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestMapMapperGenerics {
    private static final String QUERY = "select 1.0 as one, 2.0 as two, 3.0 as three from (values(null))";

    @Rule
    public SqliteDatabaseRule db = new SqliteDatabaseRule().withPlugin(new SqlObjectPlugin());

    private Jdbi jdbi;

    @Before
    public void before() {
        jdbi = db.getJdbi().registerRowMapper(new GenericMapMapperFactory());
    }

    @Test
    public void testGenericMapFluent() {
        jdbi.useHandle(h -> {
            Map<String, BigDecimal> map = h.createQuery(QUERY)
                .mapTo(new GenericType<Map<String, BigDecimal>>() {})
                .findOnly();

            assertThat(map)
                .containsOnlyKeys("one", "two", "three")
                .containsValues(new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("3.0"));
        });
    }

    @Test
    public void testGenericMapFluentConvenient() {
        jdbi.useHandle(h -> {
            Map<String, BigDecimal> map = h.createQuery(QUERY)
                .mapToGenericMap(BigDecimal.class)
                .findOnly();

            assertThat(map)
                .containsOnlyKeys("one", "two", "three")
                .containsValues(new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("3.0"));
        });
    }

    @Test
    public void testGenericMapReturnType() {
        jdbi.useExtension(Foo.class, foo -> {
            List<Map<String, BigDecimal>> list = foo.getMapList();

            assertThat(list).hasSize(1);

            Map<String, BigDecimal> map = list.get(0);

            assertThat(map)
                .containsOnlyKeys("one", "two", "three")
                .containsValues(new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("3.0"));
        });
    }

    public interface Foo {
        @SqlQuery(QUERY)
        List<Map<String, BigDecimal>> getMapList();
    }
}
