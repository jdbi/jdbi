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

import javax.annotation.Nonnull;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.jdbi.v3.core.mapper.reflect.FieldMapper;
import org.jdbi.v3.core.qualifier.Qualified;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NonNullMapperTest {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withSomething().withPlugin(new SqlObjectPlugin());

    private Jdbi jdbi;

    @Before
    public void before() {
        jdbi = db.getJdbi()
            .registerRowMapper(NullableItem.class, FieldMapper.of(NullableItem.class))
            .registerRowMapper(NonNullItem.class, FieldMapper.of(NonNullItem.class));
    }

    @Test
    public void unmarkedFieldCanBeNull() {
        NullableItem item = jdbi.withHandle(h -> h.createQuery("select null as col").mapTo(NullableItem.class).one());

        assertThat(item.col).isNull();
    }

    @Test
    public void markedFieldWithoutMapperIsMisleading() {
        NonNullItem item = jdbi.withHandle(h -> h.createQuery("select null as col").mapTo(NonNullItem.class).one());

        assertThat(item.col)
            .describedAs("your IDE would wrongly claim this field to never be null")
            .isNull();
    }

    @Test
    public void nonNullIsRespected() {
        jdbi.getConfig(ColumnMappers.class).addNonNullQualifier(Nonnull.class);

        jdbi.useHandle(h -> assertThatThrownBy(h.createQuery("select null as col").mapTo(NonNullItem.class)::one).isInstanceOf(NullPointerException.class));
    }

    public static class NullableItem {
        @ColumnName("col")
        private Integer col;
    }

    public static class NonNullItem {
        @ColumnName("col")
        @Qualified(Nonnull.class)
        @Nonnull
        private Integer col;
    }
}
