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
package org.jdbi.v3.core.mapper.reflect.internal;

import java.sql.ResultSet;

import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.StatementContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class NullDelegatingMapperTest {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    @Test
    void testNullResultReturnedForNullColumn() {
        RowMapper<String> mapper = new NullDelegatingMapper<>(1, (rs, ctx) -> "mapped", "null result");

        assertThat(mapRow("select cast(NULL as varchar) as a", mapper)).isEqualTo("null result");
        assertThat(mapRow("select 'x' as a", mapper)).isEqualTo("mapped");
    }

    @Test
    void testTwoArgumentConstructorReturnsNull() {
        RowMapper<String> mapper = new NullDelegatingMapper<>(1, (rs, ctx) -> "mapped");

        assertThat(mapRow("select cast(NULL as varchar) as a", mapper)).isNull();
    }

    @Test
    void testSpecializeKeepsColumnAndNullResult() {
        RowMapper<String> mapper = new NullDelegatingMapper<>(1, specializingTo((rs, ctx) -> "specialized"), "null result");

        assertThat(mapRow("select cast(NULL as varchar) as a", mapper)).isEqualTo("null result");
        assertThat(mapRow("select 'x' as a", mapper)).isEqualTo("specialized");
    }

    @Test
    void testSpecializeKeepsOuterCheckWhenDelegateSpecializesToNullDelegatingMapper() {
        RowMapper<String> inner = new NullDelegatingMapper<>(2, (rs, ctx) -> "specialized", "inner null result");
        RowMapper<String> mapper = new NullDelegatingMapper<>(1, specializingTo(inner), "outer null result");

        assertThat(mapRow("select cast(NULL as varchar) as a, 'x' as b", mapper)).isEqualTo("outer null result");
        assertThat(mapRow("select 'x' as a, cast(NULL as varchar) as b", mapper)).isEqualTo("inner null result");
        assertThat(mapRow("select 'x' as a, 'x' as b", mapper)).isEqualTo("specialized");
    }

    private String mapRow(String sql, RowMapper<String> mapper) {
        try (Query query = h2Extension.getSharedHandle().createQuery(sql)) {
            return query.map(mapper).one();
        }
    }

    private static RowMapper<String> specializingTo(RowMapper<String> specialized) {
        return new RowMapper<>() {
            @Override
            public String map(ResultSet rs, StatementContext ctx) {
                return "not specialized";
            }

            @Override
            public RowMapper<String> specialize(ResultSet rs, StatementContext ctx) {
                return specialized;
            }
        };
    }
}
