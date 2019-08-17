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
package org.jdbi.v3.core.mapper;

import javax.annotation.Nonnull;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NonnullColumnMapperFactoryTest {
    private static final int NULL = 0, NONNULL = 1;

    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Before
    public void before() {
        db.getJdbi()
            .configure(ColumnMappers.class, mappers -> mappers.addNonNullQualifier(Nonnull.class))
            .useHandle(h -> {
                h.createUpdate("create table foo(id int primary key, value int)").execute();
                h.createUpdate("insert into foo(id, value) values(:id, null)").bind("id", NULL).execute();
                h.createUpdate("insert into foo(id, value) values(:id, 1)").bind("id", NONNULL).execute();
            });
    }

    @Test
    public void referenceBehavior() {
        Handle h = db.getJdbi().open();

        assertThat(h.createQuery("select value from foo where id = :id")
            .bind("id", NULL)
            .mapTo(Integer.class)
            .one()).isNull();
        assertThat(h.createQuery("select value from foo where id = :id")
            .bind("id", NONNULL)
            .mapTo(Integer.class)
            .one()).isNotNull();
    }

    @Test
    public void nonNullBehavior() {
        QualifiedType<Integer> nonNullInt = QualifiedType.of(Integer.class).with(Nonnull.class);

        Handle h = db.getJdbi().open();

        assertThatThrownBy(
            () -> h.createQuery("select value from foo where id = :id")
                .bind("id", NULL)
                .mapTo(nonNullInt)
                .one())
            .isInstanceOf(NullPointerException.class)
            .hasMessage("type annotated with non-null qualifier got a null value");

        assertThat(h.createQuery("select value from foo where id = :id")
            .bind("id", NONNULL)
            .mapTo(nonNullInt)
            .one()).isNotNull();
    }
}
