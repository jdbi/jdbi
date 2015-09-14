/*
 * Copyright (C) 2004 - 2015 Brian McCallister and others
 *
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

package org.skife.jdbi.v2;

import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skife.jdbi.v2.util.BigDecimalColumnMapper;
import org.skife.jdbi.v2.util.IntegerColumnMapper;
import org.skife.jdbi.v2.util.LongColumnMapper;
import org.skife.jdbi.v2.util.StringColumnMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

@RunWith(EasyMockRunner.class)
public class BeanMapperTest {

    @Mock
    ResultSet resultSet;

    @Mock
    ResultSetMetaData resultSetMetaData;

    @Mock
    StatementContext ctx;

    BeanMapper<SampleBean> mapper = new BeanMapper<SampleBean>(SampleBean.class);

    @Test
    public void shouldSetValueOnPublicSetter() throws Exception {
        expect(resultSetMetaData.getColumnCount()).andReturn(1).anyTimes();
        expect(resultSetMetaData.getColumnLabel(1)).andReturn("longField");
        replay(resultSetMetaData);

        expect(ctx.columnMapperFor(Long.class)).andReturn(LongColumnMapper.WRAPPER);
        replay(ctx);

        expect(resultSet.getMetaData()).andReturn(resultSetMetaData);
        Long aLongVal = 100l;
        expect(resultSet.getLong(1)).andReturn(aLongVal);
        expect(resultSet.wasNull()).andReturn(false);
        replay(resultSet);

        SampleBean sampleBean = mapper.map(0, resultSet, ctx);

        assertSame(aLongVal, sampleBean.getLongField());
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
    public void shouldBeCaseInSensitiveOfColumnAndPropertyNames() throws Exception {
        expect(resultSetMetaData.getColumnCount()).andReturn(1).anyTimes();
        expect(resultSetMetaData.getColumnLabel(1)).andReturn("LoNgfielD");
        replay(resultSetMetaData);

        expect(ctx.columnMapperFor(Long.class)).andReturn(LongColumnMapper.WRAPPER);
        replay(ctx);

        expect(resultSet.getMetaData()).andReturn(resultSetMetaData);
        Long aLongVal = 100l;
        expect(resultSet.getLong(1)).andReturn(aLongVal);
        expect(resultSet.wasNull()).andReturn(false);
        replay(resultSet);
        SampleBean sampleBean = mapper.map(0, resultSet, ctx);

        assertSame(aLongVal, sampleBean.getLongField());

    }

    @Test
    public void shouldHandleNullValue() throws Exception {
        expect(resultSetMetaData.getColumnCount()).andReturn(1).anyTimes();
        expect(resultSetMetaData.getColumnLabel(1)).andReturn("LoNgfielD");
        replay(resultSetMetaData);

        expect(ctx.columnMapperFor(Long.class)).andReturn(LongColumnMapper.WRAPPER);
        replay(ctx);

        expect(resultSet.getMetaData()).andReturn(resultSetMetaData);
        expect(resultSet.getLong(1)).andReturn(0l);
        expect(resultSet.wasNull()).andReturn(true);
        replay(resultSet);

        SampleBean sampleBean = mapper.map(0, resultSet, ctx);

        assertNull(sampleBean.getLongField());

    }

    @Test
    public void shouldSetValuesOnPublicSetter() throws Exception {
        expect(resultSetMetaData.getColumnCount()).andReturn(1).anyTimes();
        expect(resultSetMetaData.getColumnLabel(1)).andReturn("longField");
        replay(resultSetMetaData);

        expect(ctx.columnMapperFor(Long.class)).andReturn(LongColumnMapper.WRAPPER);
        replay(ctx);

        expect(resultSet.getMetaData()).andReturn(resultSetMetaData);
        long expected = 1L;
        expect(resultSet.getLong(1)).andReturn(expected);
        expect(resultSet.wasNull()).andReturn(false).anyTimes();
        replay(resultSet);

        SampleBean sampleBean = mapper.map(0, resultSet, ctx);

        assertSame(expected, sampleBean.getLongField());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnProtectedSetter() throws Exception {
        expect(resultSetMetaData.getColumnCount()).andReturn(1).anyTimes();
        expect(resultSetMetaData.getColumnLabel(1)).andReturn("stringField");
        replay(resultSetMetaData);

        expect(ctx.columnMapperFor(String.class)).andReturn(StringColumnMapper.INSTANCE);
        replay(ctx);

        expect(resultSet.getMetaData()).andReturn(resultSetMetaData);
        String expected = "string";
        expect(resultSet.getString(1)).andReturn(expected);
        expect(resultSet.wasNull()).andReturn(false).anyTimes();
        replay(resultSet);

        mapper.map(0, resultSet, ctx);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnPackagePrivateSetter() throws Exception {
        expect(resultSetMetaData.getColumnCount()).andReturn(1).anyTimes();
        expect(resultSetMetaData.getColumnLabel(1)).andReturn("intField");
        replay(resultSetMetaData);

        expect(ctx.columnMapperFor(int.class)).andReturn(IntegerColumnMapper.PRIMITIVE);
        replay(ctx);

        expect(resultSet.getMetaData()).andReturn(resultSetMetaData);
        int expected = 200;
        expect(resultSet.getInt(1)).andReturn(expected);
        expect(resultSet.wasNull()).andReturn(false).anyTimes();
        replay(resultSet);

        mapper.map(0, resultSet, ctx);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnPrivateSetter() throws Exception {
        expect(resultSetMetaData.getColumnCount()).andReturn(1).anyTimes();
        expect(resultSetMetaData.getColumnLabel(1)).andReturn("bigDecimalField");
        replay(resultSetMetaData);

        expect(ctx.columnMapperFor(BigDecimal.class)).andReturn(BigDecimalColumnMapper.INSTANCE);
        replay(ctx);

        expect(resultSet.getMetaData()).andReturn(resultSetMetaData);
        BigDecimal expected = BigDecimal.ONE;
        expect(resultSet.getBigDecimal(1)).andReturn(expected);
        expect(resultSet.wasNull()).andReturn(false).anyTimes();
        replay(resultSet);

        mapper.map(0, resultSet, ctx);
    }

    @Test
    public void shouldSetValuesInSuperClassProperties() throws Exception {
        expect(resultSetMetaData.getColumnCount()).andReturn(2).anyTimes();
        expect(resultSetMetaData.getColumnLabel(1)).andReturn("longField");
        expect(resultSetMetaData.getColumnLabel(2)).andReturn("blongField");
        replay(resultSetMetaData);

        expect(ctx.columnMapperFor(Long.class)).andReturn(LongColumnMapper.WRAPPER).anyTimes();
        replay(ctx);

        expect(resultSet.getMetaData()).andReturn(resultSetMetaData);
        Long aLongVal = 100l;
        Long bLongVal = 200l;

        expect(resultSet.getLong(1)).andReturn(aLongVal);
        expect(resultSet.getLong(2)).andReturn(bLongVal);
        expect(resultSet.wasNull()).andReturn(false).anyTimes();
        replay(resultSet);

        BeanMapper<DerivedBean> mapper = new BeanMapper<DerivedBean>(DerivedBean.class);

        DerivedBean derivedBean = mapper.map(0, resultSet, ctx);

        assertEquals(aLongVal, derivedBean.getLongField());
        assertEquals(bLongVal, derivedBean.getBlongField());
    }

    @Test
    public void shouldUseRegisteredMapperForUnknownPropertyType() throws Exception {
        expect(resultSetMetaData.getColumnCount()).andReturn(2).anyTimes();
        expect(resultSetMetaData.getColumnLabel(1)).andReturn("longField");
        expect(resultSetMetaData.getColumnLabel(2)).andReturn("valueTypeField").anyTimes();
        replay(resultSetMetaData);

        expect(ctx.columnMapperFor(Long.class)).andReturn(LongColumnMapper.WRAPPER);
        expect(ctx.columnMapperFor(String.class)).andReturn(StringColumnMapper.INSTANCE);
        expect(ctx.columnMapperFor(SampleValueType.class)).andReturn(new SampleValueTypeMapper());
        replay(ctx);

        expect(resultSet.getMetaData()).andReturn(resultSetMetaData).anyTimes();
        expect(resultSet.getLong(1)).andReturn(123L);
        expect(resultSet.getString(2)).andReturn("foo");
        expect(resultSet.wasNull()).andReturn(false).anyTimes();
        replay(resultSet);

        SampleBean sampleBean = mapper.map(0, resultSet, ctx);

        assertSame(123L, sampleBean.getLongField());
        assertEquals(SampleValueType.valueOf("foo"), sampleBean.getValueTypeField());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnPropertyTypeWithoutRegisteredMapper() throws Exception {
        expect(resultSetMetaData.getColumnCount()).andReturn(2).anyTimes();
        expect(resultSetMetaData.getColumnLabel(1)).andReturn("longField");
        expect(resultSetMetaData.getColumnLabel(2)).andReturn("valueTypeField").anyTimes();
        replay(resultSetMetaData);

        expect(ctx.columnMapperFor(Long.class)).andReturn(LongColumnMapper.WRAPPER);
        expect(ctx.columnMapperFor(SampleValueType.class)).andReturn(null);
        replay(ctx);

        expect(resultSet.getMetaData()).andReturn(resultSetMetaData).anyTimes();
        expect(resultSet.getLong(1)).andReturn(123L);
        expect(resultSet.getObject(2)).andReturn(new Object());
        expect(resultSet.wasNull()).andReturn(false).anyTimes();
        replay(resultSet);

        mapper.map(0, resultSet, ctx);
    }
}
