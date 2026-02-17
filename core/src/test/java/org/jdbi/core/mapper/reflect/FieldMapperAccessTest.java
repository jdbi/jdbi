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
package org.jdbi.core.mapper.reflect;

import java.math.BigDecimal;
import java.util.List;

import org.jdbi.core.Handle;
import org.jdbi.core.SampleBean;
import org.jdbi.core.ValueType;
import org.jdbi.core.junit5.H2DatabaseExtension;
import org.jdbi.core.mapper.JoinRow;
import org.jdbi.core.mapper.JoinRowMapper;
import org.jdbi.core.mapper.ValueTypeMapper;
import org.jdbi.core.statement.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.jdbi.core.junit5.H2DatabaseExtension.USERS_INITIALIZER;

public class FieldMapperAccessTest {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(USERS_INITIALIZER);

    Handle handle;

    protected Class<?> beanClass;
    protected Class<?> derivedBeanClass;

    // Overridden by subclasses

    protected Class<?> getBeanClass() {
        return SampleBean.class;
    }

    protected Class<?> getDerivedBeanClass() {
        return DerivedBean.class;
    }

    @BeforeEach
    final void setUp() {
        this.beanClass = getBeanClass();
        this.derivedBeanClass = getDerivedBeanClass();

        this.handle = h2Extension.getSharedHandle();
        handle.registerRowMapper(beanClass, FieldMapper.of(beanClass));
        handle.registerRowMapper(derivedBeanClass, FieldMapper.of(derivedBeanClass));
        handle.registerRowMapper(IdBean.class, ConstructorMapper.of(IdBean.class));
    }

    @Test
    void shouldSetValueOnPrivateField() {
        try (Query query = handle.createQuery("SELECT id as longField FROM users order by id")) {
            List<?> fields = query.mapTo(beanClass).list();
            assertThat(fields).hasSize(2);
            assertThat(fields).extracting("longField").containsExactly(1L, 2L);
        }
    }

    @Test
    @Disabled("FieldMapper can not handle no columns mapped to it when columns are present - #2216")
    void shouldHandleEmptyResult() {
        JoinRowMapper joinMapper = JoinRowMapper.forTypes(beanClass, IdBean.class);

        try (Query query = handle.createQuery("SELECT id FROM users order by id")) {

            List<JoinRow> fields = query.map(joinMapper).list();
            assertThat(fields).hasSize(2);
        }
    }

    @Test
    void shouldBeCaseInSensitiveOfColumnAndFieldNames() {
        try (Query query = handle.createQuery("SELECT id as lOngfIELD FROM users order by id")) {
            List<?> fields = query.mapTo(beanClass).list();
            assertThat(fields).hasSize(2);
            assertThat(fields).extracting("longField").containsExactly(1L, 2L);
        }
    }

    @Test
    void shouldHandleNullValue() {
        try (Query query = handle.createQuery("SELECT NULL as longField FROM users order by id")) {
            List<?> fields = query.mapTo(beanClass).list();
            assertThat(fields).hasSize(2);
            assertThat(fields).extracting("longField").containsExactly(null, null);
        }
    }

    @Test
    void shouldThrowOnTotalMismatch() {
        try (Query query = handle.createQuery("SELECT id FROM users order by id")) {
            // note that this may clash with the shouldHandleEmptyResult test from above. If it is desirable that
            // no matches onto the mapper throws an exception, then the test above can never succeed as the delegated
            // mapper in a join row mapper can not differentiate whether it is the only one responsible for mapping
            // rows onto objects or there are additional mappers next to it in the join row mapper.
            assertThatThrownBy(() -> query.mapTo(beanClass).list()).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void shouldSetValuesOnAllFieldAccessTypes() {
        try (Query query = handle.createQuery("SELECT id as longField, "
                + "name as protectedStringField, "
                + "id as packagePrivateIntField, "
                + "id as privateBigDecimalField FROM users order by id")) {

            List<?> fields = query.mapTo(beanClass).list();
            assertThat(fields).hasSize(2);
            assertThat(fields).extracting("longField", "protectedStringField", "packagePrivateIntField", "privateBigDecimalField")
                    .containsExactly(tuple(1L, "Alice", 1, BigDecimal.valueOf(1)),
                            tuple(2L, "Bob", 2, BigDecimal.valueOf(2)));
        }
    }

    @Test
    void shouldSetValuesInSuperClassFields() {
        try (Query query = handle.createQuery("SELECT id as longField, id as blongField FROM users order by id")) {

            List<?> fields = query.mapTo(derivedBeanClass).list();
            assertThat(fields).hasSize(2);
            assertThat(fields).extracting("longField", "blongField")
                    .containsExactly(tuple(1L, 1L),
                            tuple(2L, 2L));
        }
    }

    @Test
    void shouldUseRegisteredMapperForUnknownPropertyType() {
        handle.registerColumnMapper(new ValueTypeMapper()); // valueType is the "unknown property type"

        try (Query query = handle.createQuery("SELECT id as longField, name as valueTypeField FROM users order by id")) {

            List<?> fields = query.mapTo(beanClass).list();
            assertThat(fields).hasSize(2);
            assertThat(fields).extracting("longField", "valueTypeField")
                    .containsExactly(tuple(1L, ValueType.valueOf("Alice")),
                            tuple(2L, ValueType.valueOf("Bob")));
        }

    }

    @Test
    void shouldThrowOnPropertyTypeWithoutRegisteredMapper() {

        try (Query query = handle.createQuery("SELECT id as longField, name as valueTypeField FROM users order by id")) {

            assertThatThrownBy(() -> query.mapTo(beanClass).list()).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void shouldNotThrowOnMismatchedColumns() {
        try (Query query = handle.createQuery("SELECT id as extraField, id as longField FROM users order by id")) {
            List<?> fields = query.mapTo(beanClass).list();
            assertThat(fields).hasSize(2);
            assertThat(fields).extracting("longField").containsExactly(1L, 2L);
        }
    }

    @Test
    void shouldThrowOnMismatchedColumnsStrictMatch() {
        handle.getConfig(ReflectionMappers.class).setStrictMatching(true);
        try (Query query = handle.createQuery("SELECT id as misspelledField, id as longField FROM users order by id")) {
            assertThatThrownBy(() -> query.mapTo(beanClass).list()).isInstanceOf(IllegalArgumentException.class);
        }
    }

    public static class IdBean {

        private final String id;

        public IdBean(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
}
