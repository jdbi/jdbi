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

import java.net.URI;
import java.util.List;

import org.jdbi.v3.sqlobject.SqlObjects;
import org.jdbi.v3.sqlobject.SqlQuery;
import org.jdbi.v3.sqlobject.customizers.RegisterColumnMapper;
import org.jdbi.v3.sqlobject.customizers.RegisterColumnMapperFactory;
import org.jdbi.v3.sqlobject.customizers.RegisterBeanMapper;
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
        URI uri;
        char primitiveChar;
        Character wrappedChar;

        public int getPrimitiveInt() {
            return primitiveInt;
        }

        public void setPrimitiveInt(int primitiveInt) {
            this.primitiveInt = primitiveInt;
        }

        public char getPrimitiveChar() {
            return primitiveChar;
        }

        public void setPrimitiveChar(char primitiveChar) {
            this.primitiveChar = primitiveChar;
        }

        public Character getWrappedChar() {
            return wrappedChar;
        }

        public void setWrappedChar(Character wrappedChar) {
            this.wrappedChar = wrappedChar;
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

        public URI getUri() {
            return uri;
        }

        public void setUri(URI uri) {
            this.uri = uri;
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
        @RegisterBeanMapper(SomeBean.class)
        @RegisterColumnMapper(ValueTypeMapper.class)
        @SqlQuery("select * from someBean")
        List<SomeBean> listBeans();

        @RegisterBeanMapper(SomeBean.class)
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
        h.createStatement(
            "create table someBean (" +
            "  primitiveInt integer, wrapperLong bigint, " +
            "  primitiveChar varchar(1), wrappedChar varchar(1), " +
            "  string varchar(50), valueType varchar(50), " +
            "  uri varchar(50) " +
            " )").execute();
        dao = SqlObjects.attach(h, SomeBeanDao.class);
    }

    @After
    public void dropTable() {
        h.createStatement("drop table someBean").execute();
    }

    @Test
    public void testMapPrimitiveInt() throws Exception {
        h.createStatement("insert into someBean (primitiveInt) values (15)").execute();

        List<SomeBean> beans = dao.listBeans();
        assertEquals(1, beans.size());
        assertEquals(15, beans.get(0).getPrimitiveInt());
    }

    @Test
    public void testMapPrimitiveIntFromNull() throws Exception {
        h.createStatement("insert into someBean (primitiveInt) values (null)").execute();

        List<SomeBean> beans = dao.listBeans();
        assertEquals(1, beans.size());
        assertEquals(0, beans.get(0).getPrimitiveInt());
    }

    @Test
    public void testMapPrimitiveChar() throws Exception {
        h.createStatement("insert into someBean (primitiveChar) values ('c')").execute();

        List<SomeBean> beans = dao.listBeans();
        assertEquals(1, beans.size());
        assertEquals('c', beans.get(0).getPrimitiveChar());
    }

    @Test
    public void testMapPrimitiveCharFromEmpty() throws Exception {
        h.createStatement("insert into someBean (primitiveChar) values ('')").execute();

        List<SomeBean> beans = dao.listBeans();
        assertEquals(1, beans.size());
        assertEquals('\000', beans.get(0).getPrimitiveChar());
    }

    @Test
    public void testMapPrimitiveCharFromNull() throws Exception {
        h.createStatement("insert into someBean (primitiveChar) values (null)").execute();

        List<SomeBean> beans = dao.listBeans();
        assertEquals(1, beans.size());
        assertEquals('\000', beans.get(0).getPrimitiveChar());
    }

    @Test
    public void testMapWrappedChar() throws Exception {
        h.createStatement("insert into someBean (wrappedChar) values ('c')").execute();

        List<SomeBean> beans = dao.listBeans();
        assertEquals(1, beans.size());
        assertEquals(Character.valueOf('c'), beans.get(0).getWrappedChar());
    }

    @Test
    public void testMapWrappedCharFromEmpty() throws Exception {
        h.createStatement("insert into someBean (wrappedChar) values ('')").execute();

        List<SomeBean> beans = dao.listBeans();
        assertEquals(1, beans.size());
        assertEquals(null, beans.get(0).getWrappedChar());
    }

    @Test
    public void testMapWrappedCharFromNull() throws Exception {
        h.createStatement("insert into someBean (wrappedChar) values (null)").execute();

        List<SomeBean> beans = dao.listBeans();
        assertEquals(1, beans.size());
        assertEquals(null, beans.get(0).getWrappedChar());
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

    @Test
    public void testMapUri() throws Exception {
        h.createStatement("insert into someBean (uri) values ('urn:foo')").execute();

        List<SomeBean> list = dao.listBeans();
        assertEquals(1, list.size());
        assertEquals(new URI("urn:foo"), list.get(0).getUri());
    }

    @Test
    public void testMapUriFromNull() throws Exception {
        h.createStatement("insert into someBean (uri) values (null)").execute();

        List<SomeBean> list = dao.listBeans();
        assertEquals(1, list.size());
        assertEquals(null, list.get(0).getUri());
    }
}
