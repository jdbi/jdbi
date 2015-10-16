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

import java.util.List;

import org.jdbi.v3.sqlobject.SqlObjectBuilder;
import org.jdbi.v3.sqlobject.SqlQuery;
import org.jdbi.v3.sqlobject.customizers.RegisterColumnMapper;
import org.jdbi.v3.sqlobject.customizers.RegisterColumnMapperFactory;
import org.jdbi.v3.sqlobject.helpers.MapResultAsBean;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestColumnMappers
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    public static class SomeBean
    {
        int primitiveInt;
        Long wrapperLong;
        String string;
        ValueType valueType;

        public int getPrimitiveInt() {
            return primitiveInt;
        }

        public void setPrimitiveInt(int primitiveInt) {
            this.primitiveInt = primitiveInt;
        }

        public Long getWrapperLong() {
            return wrapperLong;
        }

        public void setWrapperLong(Long wrapperLong) {
            this.wrapperLong = wrapperLong;
        }

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        public ValueType getValueType() {
            return valueType;
        }

        public void setValueType(ValueType valueType) {
            this.valueType = valueType;
        }
    }

    public interface SomeBeanDao
    {
        @MapResultAsBean
        @RegisterColumnMapper(ValueTypeMapper.class)
        @SqlQuery("select * from someBean")
        List<SomeBean> listBeans();

        @MapResultAsBean
        @RegisterColumnMapperFactory(ValueTypeMapper.Factory.class)
        @SqlQuery("select * from someBean")
        List<SomeBean> listBeansFactoryMapped();

        @RegisterColumnMapper(ValueTypeMapper.class)
        @SqlQuery("select valueType from someBean")
        List<ValueType> listValueTypes();

        @RegisterColumnMapperFactory(ValueTypeMapper.Factory.class)
        @SqlQuery("select valueType from someBean")
        List<ValueType> listValueTypesFactoryMapped();
    }

    Handle h;
    SomeBeanDao dao;

    @Before
    public void createTable() throws Exception {
        h = db.openHandle();
        h.createStatement("create table someBean (primitiveInt integer, wrapperLong bigint, string varchar(50), valueType varchar(50))").execute();
        dao = SqlObjectBuilder.attach(h, SomeBeanDao.class);
    }

    @After
    public void dropTable() {
        h.createStatement("drop table someBean").execute();
    }

    @Test
    public void testMapPrimitive() throws Exception {
        h.createStatement("insert into someBean (primitiveInt) values (15)").execute();

        List<SomeBean> beans = dao.listBeans();
        assertEquals(1, beans.size());
        assertEquals(15, beans.get(0).getPrimitiveInt());
    }

    @Test
    public void testMapPrimitiveFromNull() throws Exception {
        h.createStatement("insert into someBean (primitiveInt) values (null)").execute();

        List<SomeBean> beans = dao.listBeans();
        assertEquals(1, beans.size());
        assertEquals(0, beans.get(0).getPrimitiveInt());
    }

    @Test
    public void testMapWrapper() throws Exception {
        h.createStatement("insert into someBean (wrapperLong) values (20)").execute();

        List<SomeBean> beans = dao.listBeans();
        assertEquals(1, beans.size());
        assertEquals(Long.valueOf(20), beans.get(0).getWrapperLong());
    }

    @Test
    public void testMapWrapperFromNull() throws Exception {
        h.createStatement("insert into someBean (wrapperLong) values (null)").execute();

        List<SomeBean> beans = dao.listBeans();
        assertEquals(1, beans.size());
        assertEquals(null, beans.get(0).getWrapperLong());
    }

    @Test
    public void testMapString() throws Exception {
        h.createStatement("insert into someBean (string) values ('foo')").execute();

        List<SomeBean> beans = dao.listBeans();
        assertEquals(1, beans.size());
        assertEquals("foo", beans.get(0).getString());
    }

    @Test
    public void testMapStringFromNull() throws Exception {
        h.createStatement("insert into someBean (string) values (null)").execute();

        List<SomeBean> beans = dao.listBeans();
        assertEquals(1, beans.size());
        assertEquals(null, beans.get(0).getString());
    }

    @Test
    public void testMapValueType() throws Exception {
        h.createStatement("insert into someBean (valueType) values ('foo')").execute();

        List<SomeBean> beans = dao.listBeans();
        assertEquals(1, beans.size());
        assertEquals(ValueType.valueOf("foo"), beans.get(0).getValueType());
    }

    @Test
    public void testMapValueTypeFromNull() throws Exception {
        h.createStatement("insert into someBean (valueType) values (null)").execute();

        List<SomeBean> beans = dao.listBeans();
        assertEquals(1, beans.size());
        assertEquals(null, beans.get(0).getValueType());
    }

    @Test
    public void testMapValueTypeFromColumnMapperFactory() throws Exception {
        h.createStatement("insert into someBean (valueType) values ('foo')").execute();

        List<SomeBean> beans = dao.listBeansFactoryMapped();
        assertEquals(1, beans.size());
        assertEquals(ValueType.valueOf("foo"), beans.get(0).getValueType());
    }

    @Test
    public void testMapToValueTypeFromColumnMapper() throws Exception {
        h.createStatement("insert into someBean (valueType) values ('foo')").execute();

        List<ValueType> list = dao.listValueTypes();
        assertEquals(1, list.size());
        assertEquals(ValueType.valueOf("foo"), list.get(0));
    }

    @Test
    public void testMapToValueTypeFromColumnMapperFactory() throws Exception {
        h.createStatement("insert into someBean (valueType) values ('foo')").execute();

        List<ValueType> list = dao.listValueTypesFactoryMapped();
        assertEquals(1, list.size());
        assertEquals(ValueType.valueOf("foo"), list.get(0));
    }
}
