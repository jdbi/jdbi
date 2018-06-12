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
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.MapMapperFactory;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.config.RegisterRowMapperFactory;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestMapMapperGenerics {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Jdbi jdbi;

    @Before
    public void before() {
        jdbi = db.getJdbi();
    }

    @Test
    public void testGenericMapReturnType() {
        GenericTypes.findGenericParameter(new GenericType<Map<String, BigDecimal>>() {}.getType(), Map.class, 1);

        jdbi.useExtension(Foo.class, foo -> {
            List<Map<String, BigDecimal>> list = foo.getMapList();

            assertThat(list).hasSize(1);

            Map<String, BigDecimal> map = list.get(0);

            assertThat(map)
                .containsOnlyKeys("one", "two", "three")
                .containsValues(new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("3.0"));
        });
    }

    @RegisterRowMapperFactory(MapMapperFactory.class)
    public interface Foo {
        @SqlQuery("select 1.0 as one, 2.0 as two, 3.0 as three from (values(null))")
        List<Map<String, BigDecimal>> getMapList();
    }
}
