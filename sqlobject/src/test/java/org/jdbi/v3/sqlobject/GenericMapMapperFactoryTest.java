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
import org.jdbi.v3.core.mapper.CaseStrategy;
import org.jdbi.v3.core.mapper.GenericMapMapperFactory;
import org.jdbi.v3.core.mapper.MapMappers;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.rule.SqliteDatabaseRule;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GenericMapMapperFactoryTest {
    private static final String QUERY = "select 1.0 as one, 2.0 as two, 3.0 as three";

    @Rule
    public SqliteDatabaseRule db = new SqliteDatabaseRule().withPlugin(new SqlObjectPlugin());

    private Jdbi jdbi;

    @Before
    public void before() {
        jdbi = db.getJdbi().registerRowMapper(new GenericMapMapperFactory());
    }

    @Test
    public void canFluentMapToGenericTypeOfMap() {
        jdbi.useHandle(h -> {
            Map<String, BigDecimal> map = h.createQuery(QUERY)
                .mapTo(new GenericType<Map<String, BigDecimal>>() {})
                .one();

            assertThat(map)
                .containsOnlyKeys("one", "two", "three")
                .containsValues(new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("3.0"));
        });
    }

    @Test
    public void canFluentMapToMapWithGenericTypeForValue() {
        jdbi.useHandle(h -> {
            Map<String, BigDecimal> map = h.createQuery(QUERY)
                .mapToMap(new GenericType<BigDecimal>() {})
                .one();

            assertThat(map)
                .containsOnlyKeys("one", "two", "three")
                .containsValues(new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("3.0"));
        });
    }

    @Test
    public void canFluentMapToMapWithClassForValue() {
        jdbi.useHandle(h -> {
            Map<String, BigDecimal> map = h.createQuery(QUERY)
                .mapToMap(BigDecimal.class)
                .one();

            assertThat(map)
                .containsOnlyKeys("one", "two", "three")
                .containsValues(new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("3.0"));
        });
    }

    @Test
    public void canMapToMapWithSqlObject() {
        jdbi.useExtension(WithTypicalMap.class, withTypicalMap -> {
            List<Map<String, BigDecimal>> list = withTypicalMap.getMapList();

            assertThat(list).hasSize(1);

            Map<String, BigDecimal> map = list.get(0);

            assertThat(map)
                .containsOnlyKeys("one", "two", "three")
                .containsValues(new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("3.0"));
        });
    }

    @Test
    public void mapToMapFailsOnUnmappableClass() {
        jdbi.useHandle(h -> {
            Query query = h.createQuery(QUERY);

            assertThatThrownBy(() -> query.mapToMap(Alien.class))
                .hasMessage("no column mapper found for type " + Alien.class);
        });
    }

    @Test
    public void mapToMapFailsOnUnmappableGenericType() {
        jdbi.useHandle(h -> {
            Query query = h.createQuery(QUERY);
            GenericType<Alien> type = new GenericType<Alien>() {};

            assertThatThrownBy(() -> query.mapToMap(type))
                .hasMessage("no column mapper found for type " + type);
        });
    }

    @Test
    public void sqlObjectMethodFailsOnCallForUnmappableType() {
        jdbi.useExtension(
            WithUnsupportedMap.class, withUnsupportedMap -> assertThatThrownBy(withUnsupportedMap::getMapList)
                .hasMessage("No mapper registered for type java.util.Map<java.lang.String, " + Alien.class.getName() + ">")
        );
    }

    @Test
    public void duplicateColumnsWithoutCaseChangeCauseException() {
        jdbi.useHandle(h -> {
            h.getConfig(MapMappers.class).setCaseChange(CaseStrategy.NOP);
            ResultIterable<Map<String, BigDecimal>> query = h.createQuery(QUERY.replace("two", "one")).mapToMap(BigDecimal.class);

            assertThatThrownBy(query::findOnly)
                .hasMessageContaining("map key \"one\" (from column \"one\") appears twice");
        });
    }

    @Test
    public void duplicateKeysAfterCaseChangeCauseException() {
        jdbi.useHandle(h -> {
            h.getConfig(MapMappers.class).setCaseChange(CaseStrategy.LOWER);
            // one and ONE
            ResultIterable<Map<String, BigDecimal>> query = h.createQuery(QUERY.replace("two", "ONE")).mapToMap(BigDecimal.class);

            assertThatThrownBy(query::findOnly)
                .hasMessageContaining("map key \"one\" (from column \"ONE\") appears twice");
        });
    }

    public interface WithTypicalMap {
        @SqlQuery(QUERY)
        List<Map<String, BigDecimal>> getMapList();
    }

    public static class Alien {}

    public interface WithUnsupportedMap {
        @SqlQuery(QUERY)
        List<Map<String, Alien>> getMapList();
    }
}
