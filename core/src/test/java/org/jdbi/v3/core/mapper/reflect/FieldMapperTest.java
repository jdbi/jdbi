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
package org.jdbi.v3.core.mapper.reflect;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleAccess;
import org.jdbi.v3.core.SampleBean;
import org.jdbi.v3.core.ValueType;
import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.ValueTypeMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementContextAccess;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import javax.annotation.Nullable;

public class FieldMapperTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    @Mock
    ResultSet resultSet;

    @Mock
    ResultSetMetaData resultSetMetaData;

    Handle handle = HandleAccess.createHandle();
    StatementContext ctx = StatementContextAccess.createContext(handle);

    RowMapper<SampleBean> mapper = FieldMapper.of(SampleBean.class);

    @Before
    public void setUpMocks() throws SQLException {
        when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
    }

    private void mockColumns(String... columns) throws SQLException {
        when(resultSetMetaData.getColumnCount()).thenReturn(columns.length);
        for (int i = 0; i < columns.length; i++) {
            when(resultSetMetaData.getColumnLabel(i + 1)).thenReturn(columns[i]);
        }
    }

    @Test
    public void shouldSetValueOnPrivateField() throws Exception {
        mockColumns("longField");

        Long aLongVal = 100L;
        when(resultSet.getLong(1)).thenReturn(aLongVal);
        when(resultSet.wasNull()).thenReturn(false);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isEqualTo(aLongVal);
    }

    @Test
    public void shouldHandleEmptyResult() throws Exception {
        mockColumns();

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean).isNotNull();
    }

    @Test
    public void shouldBeCaseInSensitiveOfColumnAndFieldNames() throws Exception {
        mockColumns("LoNgfielD");

        Long aLongVal = 100L;
        when(resultSet.getLong(1)).thenReturn(aLongVal);
        when(resultSet.wasNull()).thenReturn(false);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isEqualTo(aLongVal);

    }

    @Test
    public void shouldHandleNullValue() throws Exception {
        mockColumns("LoNgfielD");

        when(resultSet.getLong(1)).thenReturn(0L);
        when(resultSet.wasNull()).thenReturn(true);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isNull();
    }

    @Test
    public void shouldThrowOnTotalMismatch() throws Exception {
        mockColumns("somethingElseEntirely");
        assertThatThrownBy(() -> mapper.map(resultSet, ctx)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldSetValuesOnAllFieldAccessTypes() throws Exception {
        mockColumns("longField", "protectedStringField", "packagePrivateIntField", "privateBigDecimalField");

        Long aLongVal = 100L;
        String aStringVal = "something";
        int aIntVal = 1;
        BigDecimal aBigDecimal = BigDecimal.TEN;
        when(resultSet.getLong(1)).thenReturn(aLongVal);
        when(resultSet.getString(2)).thenReturn(aStringVal);
        when(resultSet.getInt(3)).thenReturn(aIntVal);
        when(resultSet.getBigDecimal(4)).thenReturn(aBigDecimal);
        when(resultSet.wasNull()).thenReturn(false);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isEqualTo(aLongVal);
        assertThat(sampleBean.getPrivateBigDecimalField()).isEqualTo(aBigDecimal);
        assertThat(sampleBean.getPackagePrivateIntField()).isEqualTo(aIntVal);
        assertThat(sampleBean.getProtectedStringField()).isEqualTo(aStringVal);
    }

    @Test
    public void shouldSetValuesInSuperClassFields() throws Exception {
        mockColumns("longField", "blongField");

        Long aLongVal = 100L;
        Long bLongVal = 200L;

        when(resultSet.getLong(1)).thenReturn(aLongVal);
        when(resultSet.getLong(2)).thenReturn(bLongVal);
        when(resultSet.wasNull()).thenReturn(false);

        RowMapper<DerivedBean> mapper = FieldMapper.of(DerivedBean.class);

        DerivedBean derivedBean = mapper.map(resultSet, ctx);

        assertThat(derivedBean.getLongField()).isEqualTo(aLongVal);
        assertThat(derivedBean.getBlongField()).isEqualTo(bLongVal);
    }

    @Test
    public void shouldUseRegisteredMapperForUnknownPropertyType() throws Exception {
        handle.registerColumnMapper(new ValueTypeMapper());

        mockColumns("longField", "valueTypeField");

        when(resultSet.getLong(1)).thenReturn(123L);
        when(resultSet.getString(2)).thenReturn("foo");
        when(resultSet.wasNull()).thenReturn(false);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        Long expected = 123L;
        assertThat(sampleBean.getLongField()).isEqualTo(expected);
        assertThat(sampleBean.getValueTypeField()).isEqualTo(ValueType.valueOf("foo"));
    }

    @Test
    public void shouldThrowOnPropertyTypeWithoutRegisteredMapper() throws Exception {
        mockColumns("longField", "valueTypeField");

        when(resultSet.getLong(1)).thenReturn(123L);
        when(resultSet.getObject(2)).thenReturn(new Object());
        when(resultSet.wasNull()).thenReturn(false);

        assertThatThrownBy(() -> mapper.map(resultSet, ctx)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldNotThrowOnMismatchedColumns() throws Exception {
        mockColumns("longField", "extraColumn");

        Long expected = 666L;
        when(resultSet.getLong(1)).thenReturn(expected);
        when(resultSet.getString(2)).thenReturn("foo");

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isEqualTo(expected);
    }

    @Test
    public void shouldThrowOnMismatchedColumnsStrictMatch() throws Exception {
        ctx.getConfig(ReflectionMappers.class).setStrictMatching(true);
        mockColumns("longField", "misspelledField");
        assertThatThrownBy(() -> mapper.map(resultSet, ctx)).isInstanceOf(IllegalArgumentException.class);
    }

    static class ColumnNameThing {
        @ColumnName("id")
        int i;

        @ColumnName("name")
        String s;
    }

    @Test
    public void testColumnNameAnnotation() {
        Handle handle = dbRule.getSharedHandle();
        handle.execute("insert into something (id, name) values (1, 'foo')");

        ColumnNameThing thing = handle.createQuery("select * from something")
                .map(FieldMapper.of(ColumnNameThing.class))
                .findOnly();

        assertThat(thing.i).isEqualTo(1);
        assertThat(thing.s).isEqualTo("foo");
    }

    @Test
    public void testNested() {
        Handle handle = dbRule.getSharedHandle();
        handle.execute("insert into something (id, name) values (1, 'foo')");

        assertThat(handle
            .registerRowMapper(FieldMapper.factory(NestedThing.class))
            .select("SELECT id, name FROM something")
            .mapTo(NestedThing.class)
            .findOnly())
            .extracting("nested.i", "nested.s")
            .containsExactly(1, "foo");
    }

    @Test
    public void testNestedStrict() {
        Handle handle = dbRule.getSharedHandle();
        handle.getConfig(ReflectionMappers.class).setStrictMatching(true);
        handle.registerRowMapper(FieldMapper.factory(NestedThing.class));

        handle.execute("insert into something (id, name) values (1, 'foo')");

        assertThat(handle
            .registerRowMapper(FieldMapper.factory(NestedThing.class))
            .select("select id, name from something")
            .mapTo(NestedThing.class)
            .findOnly())
            .extracting("nested.i", "nested.s")
            .containsExactly(1, "foo");

        assertThatThrownBy(() -> handle
            .createQuery("select id, name, 1 as other from something")
            .mapTo(NestedThing.class)
            .findOnly())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("could not match fields for columns: [other]");
    }

    static class NestedThing {
        @Nested
        ColumnNameThing nested;
    }

    @Test
    public void testNestedPresent() {
        Handle handle = dbRule.getSharedHandle();
        assertThat(handle
            .registerRowMapper(FieldMapper.factory(NullableNestedThing.class))
            .select("SELECT 42 as testValue, 2 as i, '3' as s")
            .mapTo(NullableNestedThing.class)
            .findOnly())
            .extracting("testValue", "nested.i", "nested.s")
            .containsExactly(42, 2, "3");
    }

    @Test
    public void testNestedHalfPresent() {
        Handle handle = dbRule.getSharedHandle();
        assertThat(handle
            .registerRowMapper(FieldMapper.factory(NullableNestedThing.class))
            .select("SELECT 42 as testValue, '3' as s")
            .mapTo(NullableNestedThing.class)
            .findOnly())
            .extracting("testValue", "nested.i", "nested.s")
            .containsExactly(42, null, "3");
    }

    @Test
    public void testNestedAbsent() {
        Handle handle = dbRule.getSharedHandle();
        assertThat(handle
            .registerRowMapper(FieldMapper.factory(NullableNestedThing.class))
            .select("SELECT 42 as testValue")
            .mapTo(NullableNestedThing.class)
            .findOnly())
            .extracting("testValue", "nested")
            .containsExactly(42, null);
    }

    @Test
    public void testNullableColumnAbsentButNestedPresent() {
        Handle handle = dbRule.getSharedHandle();
        assertThat(handle
            .registerRowMapper(FieldMapper.factory(NullableNestedThing.class))
            .select("SELECT 's' s, 1 i")
            .mapTo(NullableNestedThing.class)
            .findOnly())
            .extracting("testValue", "nested.s", "nested.i")
            .containsExactly(null, "s", 1);
    }

    @Test
    public void testNoRecognizedColumns() {
        Handle handle = dbRule.getSharedHandle();
        assertThatThrownBy(() -> handle
            .registerRowMapper(FieldMapper.factory(NullableNestedThing.class))
            .select("SELECT 'foo' bar")
            .mapTo(NullableNestedThing.class)
            .findOnly())
            .isInstanceOf(IllegalArgumentException.class);
    }

    static class NullableNestedThing {
        @Nullable
        Integer testValue;

        @Nullable
        @Nested
        NullableThing nested;
    }

    static class NullableThing {
        @Nullable
        String s;

        @Nullable
        Integer i;
    }

    @Test
    public void testNestedPrefix() {
        Handle handle = dbRule.getSharedHandle();
        handle.execute("insert into something (id, name) values (1, 'foo')");

        assertThat(handle
            .registerRowMapper(FieldMapper.factory(NestedPrefixThing.class))
            .select("select id nested_id, name nested_name from something")
            .mapTo(NestedPrefixThing.class)
            .findOnly())
            .extracting("nested.i", "nested.s")
            .containsExactly(1, "foo");
    }

    @Test
    public void testNestedPrefixStrict() {
        Handle handle = dbRule.getSharedHandle();
        handle.getConfig(ReflectionMappers.class).setStrictMatching(true);
        handle.registerRowMapper(FieldMapper.factory(NestedPrefixThing.class));

        handle.execute("insert into something (id, name, integerValue) values (1, 'foo', 5)"); // three, sir!

        assertThat(handle
            .createQuery("select id nested_id, name nested_name, integerValue from something")
            .mapTo(NestedPrefixThing.class)
            .findOnly())
            .extracting("nested.i", "nested.s", "integerValue")
            .containsExactly(1, "foo", 5);

        assertThatThrownBy(() -> handle
            .createQuery("select id nested_id, name nested_name, 1 as other from something")
            .mapTo(NestedPrefixThing.class)
            .findOnly())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("could not match fields for columns: [other]");

        assertThatThrownBy(() -> handle
            .createQuery("select id nested_id, name nested_name, 1 as nested_other from something")
            .mapTo(NestedPrefixThing.class)
            .findOnly())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("could not match fields for columns: [nested_other]");
    }

    static class NestedPrefixThing {
        Integer integerValue;

        @Nested("nested")
        ColumnNameThing nested;
    }
}
