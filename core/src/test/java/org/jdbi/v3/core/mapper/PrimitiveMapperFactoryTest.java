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

import org.jdbi.v3.core.result.UnableToProduceResultException;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PrimitiveMapperFactoryTest {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Test
    public void defaultConfiguration() {
        assertThat(db.getJdbi().getConfig(ColumnMappers.class).getCoalesceNullPrimitivesToDefaults()).isTrue();
    }

    @Test
    public void primitivesToDefaults() {
        int value = db.getJdbi().withHandle(h ->
            h.createQuery("select null")
                .mapTo(int.class)
                .one()
        );

        assertThat(value).isZero();
    }

    @Test
    public void forbidNullPrimitives() {
        assertThatThrownBy(() -> db.getJdbi().withHandle(h ->
            h.configure(ColumnMappers.class, mappers -> mappers.setCoalesceNullPrimitivesToDefaults(false))
                .createQuery("select null as foo")
                .mapTo(int.class)
                .one()
        ))
            .isInstanceOf(UnableToProduceResultException.class)
            .hasMessageContaining("Database null values are not allowed for Java primitives")
            .hasMessageContaining("column 1 (FOO)");
    }

    @Test
    public void doesntApplyToBoxed() {
        Integer value = db.getJdbi().withHandle(h ->
            h.configure(ColumnMappers.class, mappers -> mappers.setCoalesceNullPrimitivesToDefaults(false))
                .createQuery("select null")
                .mapTo(Integer.class)
                .one()
        );

        assertThat(value).isNull();
    }
}
