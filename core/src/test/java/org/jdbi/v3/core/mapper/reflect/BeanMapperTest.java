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
import org.jdbi.v3.core.Something;
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

public class BeanMapperTest {

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

    RowMapper<SampleBean> mapper = BeanMapper.of(SampleBean.class);

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

    private void mockLongResult(long aLong) throws SQLException {
        when(resultSet.getLong(1)).thenReturn(aLong);
        when(resultSet.wasNull()).thenReturn(false);
    }

    private void mockAllNullsResult() throws SQLException {
        when(resultSet.wasNull()).thenReturn(true);
    }

    @Test
    public void shouldSetValueOnPublicSetter() throws Exception {
        mockColumns("longField");

        Long aLongVal = 100L;
        mockLongResult(aLongVal);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isEqualTo(aLongVal);
    }

    @Test
    public void shouldHandleColumNameWithUnderscores() throws Exception {
        mockColumns("LONG_FIELD");

        Long aLongVal = 100L;
        mockLongResult(aLongVal);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isEqualTo(aLongVal);
    }

    @Test
    public void shouldBeCaseInSensitiveOfColumnWithUnderscoresAndPropertyNames() throws Exception {
        mockColumns("LoNg_FiElD");

        Long aLongVal = 100L;
        mockLongResult(aLongVal);

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
    public void shouldBeCaseInSensitiveOfColumnAndPropertyNames() throws Exception {
        mockColumns("LoNgfielD");

        Long aLongVal = 100L;
        mockLongResult(aLongVal);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isEqualTo(aLongVal);
    }

    @Test
    public void shouldHandleNullValue() throws Exception {
        mockColumns("LoNgfielD");

        mockAllNullsResult();

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isNull();
    }

    @Test
    public void shouldSetValuesOnPublicSetter() throws Exception {
        mockColumns("longField");

        Long expected = 1L;
        mockLongResult(expected);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isEqualTo(expected);
    }

    @Test
    public void shouldThrowOnTotalMismatch() throws Exception {
        mockColumns("somethingElseEntirely");
        assertThatThrownBy(() -> mapper.map(resultSet, ctx)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldThrowOnProtectedSetter() throws Exception {
        mockColumns("protectedStringField");

        String expected = "string";
        when(resultSet.getString(1)).thenReturn(expected);
        when(resultSet.wasNull()).thenReturn(false);

        assertThatThrownBy(() -> mapper.map(resultSet, ctx)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldThrowOnPackagePrivateSetter() throws Exception {
        mockColumns("packagePrivateIntField");

        when(resultSet.getInt(1)).thenReturn(200);
        when(resultSet.wasNull()).thenReturn(false);

        assertThatThrownBy(() -> mapper.map(resultSet, ctx)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldThrowOnPrivateSetter() throws Exception {
        mockColumns("privateBigDecimalField");

        when(resultSet.getBigDecimal(1)).thenReturn(BigDecimal.ONE);
        when(resultSet.wasNull()).thenReturn(false);

        assertThatThrownBy(() -> mapper.map(resultSet, ctx)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldSetValuesInSuperClassProperties() throws Exception {
        mockColumns("longField", "blongField");

        Long aLongVal = 100L;
        Long bLongVal = 200L;

        when(resultSet.getLong(1)).thenReturn(aLongVal);
        when(resultSet.getLong(2)).thenReturn(bLongVal);
        when(resultSet.wasNull()).thenReturn(false);

        RowMapper<DerivedBean> mapper = BeanMapper.of(DerivedBean.class);

        DerivedBean derivedBean = mapper.map(resultSet, ctx);

        assertThat(derivedBean.getLongField()).isEqualTo(aLongVal);
        assertThat(derivedBean.getBlongField()).isEqualTo(bLongVal);
    }

    @Test
    public void shouldUseRegisteredMapperForUnknownPropertyType() throws Exception {
        handle.registerColumnMapper(new ValueTypeMapper());

        mockColumns("longField", "valueTypeField");
        Long expected = 123L;

        when(resultSet.getLong(1)).thenReturn(expected);
        when(resultSet.getString(2)).thenReturn("foo");
        when(resultSet.wasNull()).thenReturn(false);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

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

    static class ColumnNameBean {
        int i;
        String s;

        @ColumnName("id")
        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }

        public String getS() {
            return s;
        }

        @ColumnName("name")
        public void setS(String s) {
            this.s = s;
        }
    }

    @Test
    public void testColumnNameAnnotation() {
        Handle handle = dbRule.getSharedHandle();
        handle.registerRowMapper(BeanMapper.factory(ColumnNameBean.class));

        handle.execute("insert into something (id, name) values (1, 'foo')");

        ColumnNameBean bean = handle.createQuery("select * from something")
                .mapTo(ColumnNameBean.class)
                .findOnly();

        assertThat(bean.getI()).isEqualTo(1);
        assertThat(bean.getS()).isEqualTo("foo");
    }

    @Test
    public void testNested() {
        Handle handle = dbRule.getSharedHandle();
        handle.registerRowMapper(BeanMapper.factory(NestedBean.class));

        handle.execute("insert into something (id, name) values (1, 'foo')");

        assertThat(handle
            .createQuery("select id, name from something")
            .mapTo(NestedBean.class)
            .findOnly())
            .extracting("nested.id", "nested.name")
            .containsExactly(1, "foo");
    }

    @Test
    public void testNestedStrict() {
        Handle handle = dbRule.getSharedHandle();
        handle.getConfig(ReflectionMappers.class).setStrictMatching(true);
        handle.registerRowMapper(BeanMapper.factory(NestedBean.class));

        handle.execute("insert into something (id, name) values (1, 'foo')");

        assertThat(handle
            .createQuery("select id, name from something")
            .mapTo(NestedBean.class)
            .findOnly())
            .extracting("nested.id", "nested.name")
            .containsExactly(1, "foo");

        assertThatThrownBy(() -> handle
            .createQuery("select id, name, 1 as other from something")
            .mapTo(NestedBean.class)
            .findOnly())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("could not match properties for columns: [other]");
    }

    @Test
    public void testNestedNotReturned() {
        Handle handle = dbRule.getSharedHandle();
        handle.registerRowMapper(BeanMapper.factory(NestedBean.class));
        assertThat(handle
            .createQuery("select 42 as testValue")
            .mapTo(NestedBean.class)
            .findOnly())
            .extracting("testValue", "nested")
            .containsExactly(42, null);
    }

    static class NestedBean {
        private Integer testValue;
        private Something nested;

        public Integer getTestValue() {
            return testValue;
        }

        public void setTestValue(Integer testValue) {
            this.testValue = testValue;
        }

        @Nested
        public Something getNested() {
            return nested;
        }

        public void setNested(Something nested) {
            this.nested = nested;
        }
    }

    @Test
    public void testNestedPrefix() {
        Handle handle = dbRule.getSharedHandle();
        handle.registerRowMapper(BeanMapper.factory(NestedPrefixBean.class));

        handle.execute("insert into something (id, name) values (1, 'foo')");

        assertThat(handle
            .createQuery("select id nested_id, name nested_name from something")
            .mapTo(NestedPrefixBean.class)
            .findOnly())
            .extracting("nested.id", "nested.name")
            .containsExactly(1, "foo");
    }

    @Test
    public void testNestedPrefixStrict() {
        Handle handle = dbRule.getSharedHandle();
        handle.getConfig(ReflectionMappers.class).setStrictMatching(true);
        handle.registerRowMapper(BeanMapper.factory(NestedPrefixBean.class));

        handle.execute("insert into something (id, name, integerValue) values (1, 'foo', 5)"); // three, sir!

        assertThat(handle
            .createQuery("select id nested_id, name nested_name, integerValue from something")
            .mapTo(NestedPrefixBean.class)
            .findOnly())
            .extracting("nested.id", "nested.name", "integerValue")
            .containsExactly(1, "foo", 5);

        assertThatThrownBy(() -> handle
            .createQuery("select id nested_id, name nested_name, 1 as other from something")
            .mapTo(NestedPrefixBean.class)
            .findOnly())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("could not match properties for columns: [other]");

        assertThatThrownBy(() -> handle
            .createQuery("select id nested_id, name nested_name, 1 as nested_other from something")
            .mapTo(NestedPrefixBean.class)
            .findOnly())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("could not match properties for columns: [nested_other]");
    }

    @Test
    public void testNestedPrefixNotReturned() {
        Handle handle = dbRule.getSharedHandle();
        handle.registerRowMapper(BeanMapper.factory(NestedPrefixBean.class));
        assertThat(handle
            .createQuery("select 42 as integerValue")
            .mapTo(NestedPrefixBean.class)
            .findOnly())
            .extracting("integerValue", "nested")
            .containsExactly(42, null);
    }

    static class NestedPrefixBean {
        private Integer integerValue;
        private Something nested;

        public Integer getIntegerValue() {
            return integerValue;
        }

        public void setIntegerValue(Integer integerValue) {
            this.integerValue = integerValue;
        }

        public Something getNested() {
            return nested;
        }

        @Nested("nested")
        public void setNested(Something nested) {
            this.nested = nested;
        }
    }
}
