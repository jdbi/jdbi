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

import org.jdbi.v3.mapper.FieldMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;


public class FieldMapperTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    ResultSet resultSet;

    @Mock
    ResultSetMetaData resultSetMetaData;

    JdbiConfig config = new JdbiConfig();
    StatementContext ctx = new StatementContext(config);

    FieldMapper<SampleBean> mapper = new FieldMapper<>(SampleBean.class);
    
    @Before
    public void setUpMocks() throws SQLException {
        when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
    }

    @Test
    public void shouldSetValueOnPrivateField() throws Exception {
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("longField");

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
    public void shouldBeCaseInSensitiveOfColumnAndFieldNames() throws Exception {
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
    public void shouldSetValuesOnAllFieldAccessTypes() throws Exception {
        when(resultSetMetaData.getColumnCount()).thenReturn(4);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("longField");
        when(resultSetMetaData.getColumnLabel(2)).thenReturn("protectedStringField");
        when(resultSetMetaData.getColumnLabel(3)).thenReturn("packagePrivateIntField");
        when(resultSetMetaData.getColumnLabel(4)).thenReturn("privateBigDecimalField");

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

        assertEquals(aLongVal, sampleBean.getLongField());
        assertEquals(aBigDecimal, sampleBean.getPrivateBigDecimalField());
        assertEquals(aIntVal, sampleBean.getPackagePrivateIntField());
        assertEquals(aStringVal, sampleBean.getProtectedStringField());
    }

    @Test
    public void shouldSetValuesInSuperClassFields() throws Exception {
        when(resultSetMetaData.getColumnCount()).thenReturn(2);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("longField");
        when(resultSetMetaData.getColumnLabel(2)).thenReturn("blongField");

        Long aLongVal = 100L;
        Long bLongVal = 200L;

        when(resultSet.getLong(1)).thenReturn(aLongVal);
        when(resultSet.getLong(2)).thenReturn(bLongVal);
        when(resultSet.wasNull()).thenReturn(false);

        FieldMapper<DerivedBean> mapper = new FieldMapper<>(DerivedBean.class);

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
