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

package org.jdbi.v3.sqlobject;

import java.net.URI;
import java.util.List;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.ValueType;
import org.jdbi.v3.core.mapper.ValueTypeMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterColumnMapper;
import org.jdbi.v3.sqlobject.config.RegisterColumnMapperFactory;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestColumnMappers {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    public static class SomeBean {
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

    public interface SomeBeanDao {
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
        h = dbRule.openHandle();
        h.createUpdate(
            "create table someBean ("
                + "  primitiveInt integer, wrapperLong bigint, "
                + "  primitiveChar varchar(1), wrappedChar varchar(1), "
                + "  string varchar(50), valueType varchar(50), "
                + "  uri varchar(50) "
                + ")").execute();
        dao = h.attach(SomeBeanDao.class);
    }

    @After
    public void dropTable() {
        h.createUpdate("drop table someBean").execute();
    }

    @Test
    public void testMapPrimitiveInt() throws Exception {
        h.createUpdate("insert into someBean (primitiveInt) values (15)").execute();

        List<SomeBean> beans = dao.listBeans();
        assertThat(beans).extracting(SomeBean::getPrimitiveInt).containsExactly(15);
    }

    @Test
    public void testMapPrimitiveIntFromNull() throws Exception {
        h.createUpdate("insert into someBean (primitiveInt) values (null)").execute();

        List<SomeBean> beans = dao.listBeans();
        assertThat(beans).extracting(SomeBean::getPrimitiveInt).containsExactly(0);
    }

    @Test
    public void testMapPrimitiveChar() throws Exception {
        h.createUpdate("insert into someBean (primitiveChar) values ('c')").execute();

        List<SomeBean> beans = dao.listBeans();
        assertThat(beans).extracting(SomeBean::getPrimitiveChar).containsExactly('c');
    }

    @Test
    public void testMapPrimitiveCharFromEmpty() throws Exception {
        h.createUpdate("insert into someBean (primitiveChar) values ('')").execute();

        List<SomeBean> beans = dao.listBeans();
        assertThat(beans).extracting(SomeBean::getPrimitiveChar).containsExactly('\000');
    }

    @Test
    public void testMapPrimitiveCharFromNull() throws Exception {
        h.createUpdate("insert into someBean (primitiveChar) values (null)").execute();

        List<SomeBean> beans = dao.listBeans();
        assertThat(beans).extracting(SomeBean::getPrimitiveChar).containsExactly('\000');
    }

    @Test
    public void testMapWrappedChar() throws Exception {
        h.createUpdate("insert into someBean (wrappedChar) values ('c')").execute();

        List<SomeBean> beans = dao.listBeans();
        assertThat(beans).extracting(SomeBean::getWrappedChar).containsExactly('c');
    }

    @Test
    public void testMapWrappedCharFromEmpty() throws Exception {
        h.createUpdate("insert into someBean (wrappedChar) values ('')").execute();

        List<SomeBean> beans = dao.listBeans();
        assertThat(beans).extracting(SomeBean::getWrappedChar).hasSize(1).containsNull();
    }

    @Test
    public void testMapWrappedCharFromNull() throws Exception {
        h.createUpdate("insert into someBean (wrappedChar) values (null)").execute();

        List<SomeBean> beans = dao.listBeans();
        assertThat(beans).extracting(SomeBean::getWrappedChar).hasSize(1).containsNull();
    }

    @Test
    public void testMapWrapper() throws Exception {
        h.createUpdate("insert into someBean (wrapperLong) values (20)").execute();

        List<SomeBean> beans = dao.listBeans();
        assertThat(beans).extracting(SomeBean::getWrapperLong).containsExactly(20L);
    }

    @Test
    public void testMapWrapperFromNull() throws Exception {
        h.createUpdate("insert into someBean (wrapperLong) values (null)").execute();

        List<SomeBean> beans = dao.listBeans();
        assertThat(beans).extracting(SomeBean::getWrapperLong).hasSize(1).containsNull();
    }

    @Test
    public void testMapString() throws Exception {
        h.createUpdate("insert into someBean (string) values ('foo')").execute();

        List<SomeBean> beans = dao.listBeans();
        assertThat(beans).extracting(SomeBean::getString).containsExactly("foo");
    }

    @Test
    public void testMapStringFromNull() throws Exception {
        h.createUpdate("insert into someBean (string) values (null)").execute();

        List<SomeBean> beans = dao.listBeans();
        assertThat(beans).extracting(SomeBean::getString).hasSize(1).containsNull();
    }

    @Test
    public void testMapValueType() throws Exception {
        h.createUpdate("insert into someBean (valueType) values ('foo')").execute();

        List<SomeBean> beans = dao.listBeans();
        assertThat(beans).extracting(SomeBean::getValueType).containsExactly(ValueType.valueOf("foo"));
    }

    @Test
    public void testMapValueTypeFromNull() throws Exception {
        h.createUpdate("insert into someBean (valueType) values (null)").execute();

        List<SomeBean> beans = dao.listBeans();
        assertThat(beans).extracting(SomeBean::getValueType).hasSize(1).containsNull();
    }

    @Test
    public void testMapValueTypeFromColumnMapperFactory() throws Exception {
        h.createUpdate("insert into someBean (valueType) values ('foo')").execute();

        List<SomeBean> beans = dao.listBeansFactoryMapped();
        assertThat(beans).extracting(SomeBean::getValueType).containsExactly(ValueType.valueOf("foo"));
    }

    @Test
    public void testMapToValueTypeFromColumnMapper() throws Exception {
        h.createUpdate("insert into someBean (valueType) values ('foo')").execute();

        List<ValueType> list = dao.listValueTypes();
        assertThat(list).containsExactly(ValueType.valueOf("foo"));
    }

    @Test
    public void testMapToValueTypeFromColumnMapperFactory() throws Exception {
        h.createUpdate("insert into someBean (valueType) values ('foo')").execute();

        List<ValueType> list = dao.listValueTypesFactoryMapped();
        assertThat(list).containsExactly(ValueType.valueOf("foo"));
    }

    @Test
    public void testMapUri() throws Exception {
        h.createUpdate("insert into someBean (uri) values ('urn:foo')").execute();

        List<SomeBean> list = dao.listBeans();
        assertThat(list).extracting(SomeBean::getUri).containsExactly(new URI("urn:foo"));
    }

    @Test
    public void testMapUriFromNull() throws Exception {
        h.createUpdate("insert into someBean (uri) values (null)").execute();

        List<SomeBean> list = dao.listBeans();
        assertThat(list).extracting(SomeBean::getUri).hasSize(1).containsNull();
    }
}
