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

package org.jdbi.v3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class BeanMapperTest {
    
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    ResultSet resultSet;

    @Mock
    ResultSetMetaData resultSetMetaData;

    JdbiConfig config = new JdbiConfig();
    StatementContext ctx = new StatementContext(config);

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

        assertEquals(aLongVal, sampleBean.getLongField());
    }

    @Test
    public void shouldHandleColumNameWithUnderscores() throws Exception {
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("LONG_FIELD");

        Long aLongVal = 100L;
        when(resultSet.getLong(1)).thenReturn(aLongVal);
        when(resultSet.wasNull()).thenReturn(false);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertEquals(aLongVal, sampleBean.getLongField());
    }

    @Test
    public void shouldBeCaseInSensitiveOfColumnWithUnderscoresAndPropertyNames() throws Exception {
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("LoNg_FiElD");

        Long aLongVal = 100L;
        when(resultSet.getLong(1)).thenReturn(aLongVal);
        when(resultSet.wasNull()).thenReturn(false);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertEquals(aLongVal, sampleBean.getLongField());
    }

    @Test
    public void shouldHandleEmptyResult() throws Exception {
        when(resultSetMetaData.getColumnCount()).thenReturn(0);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertNotNull(sampleBean);
    }

    @Test
    public void shouldBeCaseInSensitiveOfColumnAndPropertyNames() throws Exception {
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("LoNgfielD");

        Long aLongVal = 100L;
        when(resultSet.getLong(1)).thenReturn(aLongVal);
        when(resultSet.wasNull()).thenReturn(false);
        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertEquals(aLongVal, sampleBean.getLongField());

    }

    @Test
    public void shouldHandleNullValue() throws Exception {
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("LoNgfielD");

        when(resultSet.getLong(1)).thenReturn(0L);
        when(resultSet.wasNull()).thenReturn(true);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertNull(sampleBean.getLongField());

    }

    @Test
    public void shouldSetValuesOnPublicSetter() throws Exception {
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("longField");

        Long expected = 1L;
        when(resultSet.getLong(1)).thenReturn(expected);
        when(resultSet.wasNull()).thenReturn(false);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertEquals(expected, sampleBean.getLongField());
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

        assertEquals(aLongVal, derivedBean.getLongField());
        assertEquals(bLongVal, derivedBean.getBlongField());
    }

    @Test
    public void shouldUseRegisteredMapperForUnknownPropertyType() throws Exception {
        config.mappingRegistry.addColumnMapper(new ValueTypeMapper());

        when(resultSetMetaData.getColumnCount()).thenReturn(2);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("longField");
        when(resultSetMetaData.getColumnLabel(2)).thenReturn("valueTypeField");

        when(resultSet.getLong(1)).thenReturn(123L);
        when(resultSet.getString(2)).thenReturn("foo");
        when(resultSet.wasNull()).thenReturn(false);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        Long expected = 123L;
        assertEquals(expected, sampleBean.getLongField());
        assertEquals(ValueType.valueOf("foo"), sampleBean.getValueTypeField());
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
}
