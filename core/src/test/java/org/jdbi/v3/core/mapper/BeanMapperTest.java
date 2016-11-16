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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.jdbi.v3.core.ColumnName;
import org.jdbi.v3.core.DerivedBean;
import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.JdbiAccess;
import org.jdbi.v3.core.MappingRegistry;
import org.jdbi.v3.core.SampleBean;
import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.ValueType;
import org.jdbi.v3.core.ValueTypeMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class BeanMapperTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Mock
    ResultSet resultSet;

    @Mock
    ResultSetMetaData resultSetMetaData;

    Handle handle = JdbiAccess.createHandle();
    StatementContext ctx = JdbiAccess.createContext(handle);

    BeanMapper<SampleBean> mapper = new BeanMapper<>(SampleBean.class);

    @Before
    public void setUpMocks() throws SQLException {
        when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
    }

    @Test
    public void shouldSetValueOnPublicSetter() throws Exception {
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("longField");

        Long aLongVal = 100L;
        when(resultSet.getLong(1)).thenReturn(aLongVal);
        when(resultSet.wasNull()).thenReturn(false);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isEqualTo(aLongVal);
    }

    @Test
    public void shouldHandleColumNameWithUnderscores() throws Exception {
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("LONG_FIELD");

        Long aLongVal = 100L;
        when(resultSet.getLong(1)).thenReturn(aLongVal);
        when(resultSet.wasNull()).thenReturn(false);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isEqualTo(aLongVal);
    }

    @Test
    public void shouldBeCaseInSensitiveOfColumnWithUnderscoresAndPropertyNames() throws Exception {
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("LoNg_FiElD");

        Long aLongVal = 100L;
        when(resultSet.getLong(1)).thenReturn(aLongVal);
        when(resultSet.wasNull()).thenReturn(false);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isEqualTo(aLongVal);
    }

    @Test
    public void shouldHandleEmptyResult() throws Exception {
        when(resultSetMetaData.getColumnCount()).thenReturn(0);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean).isNotNull();
    }

    @Test
    public void shouldBeCaseInSensitiveOfColumnAndPropertyNames() throws Exception {
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("LoNgfielD");

        Long aLongVal = 100L;
        when(resultSet.getLong(1)).thenReturn(aLongVal);
        when(resultSet.wasNull()).thenReturn(false);
        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isEqualTo(aLongVal);
    }

    @Test
    public void shouldHandleNullValue() throws Exception {
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("LoNgfielD");

        when(resultSet.getLong(1)).thenReturn(0L);
        when(resultSet.wasNull()).thenReturn(true);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isNull();
    }

    @Test
    public void shouldSetValuesOnPublicSetter() throws Exception {
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("longField");

        Long expected = 1L;
        when(resultSet.getLong(1)).thenReturn(expected);
        when(resultSet.wasNull()).thenReturn(false);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isEqualTo(expected);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnProtectedSetter() throws Exception {
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("protectedStringField");

        String expected = "string";
        when(resultSet.getString(1)).thenReturn(expected);
        when(resultSet.wasNull()).thenReturn(false);

        mapper.map(resultSet, ctx);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnPackagePrivateSetter() throws Exception {
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("packagePrivateIntField");

        int expected = 200;
        when(resultSet.getInt(1)).thenReturn(expected);
        when(resultSet.wasNull()).thenReturn(false);

        mapper.map(resultSet, ctx);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnPrivateSetter() throws Exception {
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("privateBigDecimalField");

        BigDecimal expected = BigDecimal.ONE;
        when(resultSet.getBigDecimal(1)).thenReturn(expected);
        when(resultSet.wasNull()).thenReturn(false);

        mapper.map(resultSet, ctx);
    }

    @Test
    public void shouldSetValuesInSuperClassProperties() throws Exception {
        when(resultSetMetaData.getColumnCount()).thenReturn(2);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("longField");
        when(resultSetMetaData.getColumnLabel(2)).thenReturn("blongField");

        Long aLongVal = 100L;
        Long bLongVal = 200L;

        when(resultSet.getLong(1)).thenReturn(aLongVal);
        when(resultSet.getLong(2)).thenReturn(bLongVal);
        when(resultSet.wasNull()).thenReturn(false);

        BeanMapper<DerivedBean> mapper = new BeanMapper<>(DerivedBean.class);

        DerivedBean derivedBean = mapper.map(resultSet, ctx);

        assertThat(derivedBean.getLongField()).isEqualTo(aLongVal);
        assertThat(derivedBean.getBlongField()).isEqualTo(bLongVal);
    }

    @Test
    public void shouldUseRegisteredMapperForUnknownPropertyType() throws Exception {
        handle.getConfig(MappingRegistry.class).registerColumnMapper(new ValueTypeMapper());

        when(resultSetMetaData.getColumnCount()).thenReturn(2);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("longField");
        when(resultSetMetaData.getColumnLabel(2)).thenReturn("valueTypeField");

        when(resultSet.getLong(1)).thenReturn(123L);
        when(resultSet.getString(2)).thenReturn("foo");
        when(resultSet.wasNull()).thenReturn(false);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        Long expected = 123L;
        assertThat(sampleBean.getLongField()).isEqualTo(expected);
        assertThat(sampleBean.getValueTypeField()).isEqualTo(ValueType.valueOf("foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnPropertyTypeWithoutRegisteredMapper() throws Exception {
        when(resultSetMetaData.getColumnCount()).thenReturn(2);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("longField");
        when(resultSetMetaData.getColumnLabel(2)).thenReturn("valueTypeField");

        when(resultSet.getLong(1)).thenReturn(123L);
        when(resultSet.getObject(2)).thenReturn(new Object());
        when(resultSet.wasNull()).thenReturn(false);

        mapper.map(resultSet, ctx);
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
        Handle handle = db.getSharedHandle();
        handle.getConfig(MappingRegistry.class).registerRowMapper(BeanMapper.of(ColumnNameBean.class));

        handle.execute("insert into something (id, name) values (1, 'foo')");

        ColumnNameBean bean = handle.createQuery("select * from something")
                .mapTo(ColumnNameBean.class)
                .findOnly();

        assertThat(bean.getI()).isEqualTo(1);
        assertThat(bean.getS()).isEqualTo("foo");
    }
}
