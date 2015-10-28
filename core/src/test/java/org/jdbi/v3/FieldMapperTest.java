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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Collections;

import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(EasyMockRunner.class)
public class FieldMapperTest {

    @Mock
    ResultSet resultSet;

    @Mock
    ResultSetMetaData resultSetMetaData;

    TestingStatementContext ctx = new TestingStatementContext(Collections.emptyMap());

    FieldMapper<SampleBean> mapper = new FieldMapper<SampleBean>(SampleBean.class);

    @Test
    public void shouldSetValueOnPrivateField() throws Exception {
        expect(resultSetMetaData.getColumnCount()).andReturn(1).anyTimes();
        expect(resultSetMetaData.getColumnLabel(1)).andReturn("longField");
        replay(resultSetMetaData);

        expect(resultSet.getMetaData()).andReturn(resultSetMetaData);
        Long aLongVal = 100l;
        expect(resultSet.getLong(1)).andReturn(aLongVal);
        expect(resultSet.wasNull()).andReturn(false);
        replay(resultSet);

        SampleBean sampleBean = mapper.map(0, resultSet, ctx);

        assertEquals(aLongVal, sampleBean.getLongField());
    }

    @Test
    public void shouldHandleEmptyResult() throws Exception {
        expect(resultSetMetaData.getColumnCount()).andReturn(0);
        replay(resultSetMetaData);

        expect(resultSet.getMetaData()).andReturn(resultSetMetaData);
        replay(resultSet);

        SampleBean sampleBean = mapper.map(0, resultSet, ctx);

        assertNotNull(sampleBean);
    }

    @Test
    public void shouldBeCaseInSensitiveOfColumnAndFieldNames() throws Exception {
        expect(resultSetMetaData.getColumnCount()).andReturn(1).anyTimes();
        expect(resultSetMetaData.getColumnLabel(1)).andReturn("LoNgfielD");
        replay(resultSetMetaData);

        expect(resultSet.getMetaData()).andReturn(resultSetMetaData);
        Long aLongVal = 100l;
        expect(resultSet.getLong(1)).andReturn(aLongVal);
        expect(resultSet.wasNull()).andReturn(false);
        replay(resultSet);

        SampleBean sampleBean = mapper.map(0, resultSet, ctx);

        assertEquals(aLongVal, sampleBean.getLongField());

    }

    @Test
    public void shouldHandleNullValue() throws Exception {
        expect(resultSetMetaData.getColumnCount()).andReturn(1).anyTimes();
        expect(resultSetMetaData.getColumnLabel(1)).andReturn("LoNgfielD");
        replay(resultSetMetaData);

        expect(resultSet.getMetaData()).andReturn(resultSetMetaData);
        expect(resultSet.getLong(1)).andReturn(0l);
        expect(resultSet.wasNull()).andReturn(true);
        replay(resultSet);

        SampleBean sampleBean = mapper.map(0, resultSet, ctx);

        assertNull(sampleBean.getLongField());

    }

    @Test
    public void shouldSetValuesOnAllFieldAccessTypes() throws Exception {
        expect(resultSetMetaData.getColumnCount()).andReturn(4).anyTimes();
        expect(resultSetMetaData.getColumnLabel(1)).andReturn("longField");
        expect(resultSetMetaData.getColumnLabel(2)).andReturn("protectedStringField");
        expect(resultSetMetaData.getColumnLabel(3)).andReturn("packagePrivateIntField");
        expect(resultSetMetaData.getColumnLabel(4)).andReturn("privateBigDecimalField");
        replay(resultSetMetaData);

        Long aLongVal = 100l;
        String aStringVal = "something";
        int aIntVal = 1;
        BigDecimal aBigDecimal = BigDecimal.TEN;
        expect(resultSet.getMetaData()).andReturn(resultSetMetaData);
        expect(resultSet.getLong(1)).andReturn(aLongVal);
        expect(resultSet.getString(2)).andReturn(aStringVal);
        expect(resultSet.getInt(3)).andReturn(aIntVal);
        expect(resultSet.getBigDecimal(4)).andReturn(aBigDecimal);
        expect(resultSet.wasNull()).andReturn(false).anyTimes();
        replay(resultSet);

        SampleBean sampleBean = mapper.map(0, resultSet, ctx);

        assertEquals(aLongVal, sampleBean.getLongField());
        assertEquals(aBigDecimal, sampleBean.getPrivateBigDecimalField());
        assertEquals(aIntVal, sampleBean.getPackagePrivateIntField());
        assertEquals(aStringVal, sampleBean.getProtectedStringField());
    }

    @Test
    public void shouldSetValuesInSuperClassFields() throws Exception {
        expect(resultSetMetaData.getColumnCount()).andReturn(2).anyTimes();
        expect(resultSetMetaData.getColumnLabel(1)).andReturn("longField");
        expect(resultSetMetaData.getColumnLabel(2)).andReturn("blongField");
        replay(resultSetMetaData);

        expect(resultSet.getMetaData()).andReturn(resultSetMetaData);
        Long aLongVal = 100l;
        Long bLongVal = 200l;

        expect(resultSet.getLong(1)).andReturn(aLongVal);
        expect(resultSet.getLong(2)).andReturn(bLongVal);
        expect(resultSet.wasNull()).andReturn(false).anyTimes();
        replay(resultSet);

        FieldMapper<DerivedBean> mapper = new FieldMapper<DerivedBean>(DerivedBean.class);

        DerivedBean derivedBean = mapper.map(0, resultSet, ctx);

        assertEquals(aLongVal, derivedBean.getLongField());
        assertEquals(bLongVal, derivedBean.getBlongField());
    }

    @Test
    public void shouldUseRegisteredMapperForUnknownPropertyType() throws Exception {
        ctx.registerColumnMapper(new ValueTypeMapper());

        expect(resultSetMetaData.getColumnCount()).andReturn(2).anyTimes();
        expect(resultSetMetaData.getColumnLabel(1)).andReturn("longField");
        expect(resultSetMetaData.getColumnLabel(2)).andReturn("valueTypeField").anyTimes();
        replay(resultSetMetaData);

        expect(resultSet.getMetaData()).andReturn(resultSetMetaData).anyTimes();
        expect(resultSet.getLong(1)).andReturn(123L);
        expect(resultSet.getString(2)).andReturn("foo");
        expect(resultSet.wasNull()).andReturn(false).anyTimes();
        replay(resultSet);

        SampleBean sampleBean = mapper.map(0, resultSet, ctx);

        Long expected = 123L;
        assertEquals(expected, sampleBean.getLongField());
        assertEquals(ValueType.valueOf("foo"), sampleBean.getValueTypeField());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnPropertyTypeWithoutRegisteredMapper() throws Exception {
        expect(resultSetMetaData.getColumnCount()).andReturn(2).anyTimes();
        expect(resultSetMetaData.getColumnLabel(1)).andReturn("longField");
        expect(resultSetMetaData.getColumnLabel(2)).andReturn("valueTypeField").anyTimes();
        replay(resultSetMetaData);

        expect(resultSet.getMetaData()).andReturn(resultSetMetaData).anyTimes();
        expect(resultSet.getLong(1)).andReturn(123L);
        expect(resultSet.getObject(2)).andReturn(new Object());
        expect(resultSet.wasNull()).andReturn(false).anyTimes();
        replay(resultSet);

        mapper.map(0, resultSet, ctx);
    }
}
