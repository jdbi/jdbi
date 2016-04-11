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

package org.skife.jdbi.v2.tweak;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBITestCase;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.ValueType;
import org.skife.jdbi.v2.ValueTypeMapper;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterColumnMapper;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapperFactory;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestBeanMapperFactory extends DBITestCase
{
    public static class TestBean
    {
        private ValueType valueType;

        public ValueType getValueType() {
            return valueType;
        }

        public void setValueType(ValueType valueType) {
            this.valueType = valueType;
        }
    }

    public enum TestEnum {
        foo,
        bar;
    }

    @RegisterMapperFactory(BeanMapperFactory.class)
    @RegisterColumnMapper(ValueTypeMapper.class)
    public interface TestDao
    {
        @SqlQuery("select * from testBean")
        List<TestBean> listBeans();

        @SqlQuery("select * from testBean")
        List<String> listStrings();

        @SqlQuery("select * from testBean")
        List<TestEnum> listEnums();

        @SqlQuery("select * from testBean")
        List<ValueType> listValueTypes();
    }

    Handle h;
    TestDao dao;

    @Before
    public void createTable() throws Exception {
        h = openHandle();
        h.createStatement("create table testBean (valueType varchar(50))").execute();
        dao = h.attach(TestDao.class);
    }

    @After
    public void dropTable() {
        h.createStatement("drop table testBean").execute();
    }

    @Test
    public void testMapBean() {
        h.createStatement("insert into testBean (valueType) values ('foo')").execute();

        List<TestBean> beans = dao.listBeans();
        assertEquals(1, beans.size());
        assertEquals(ValueType.valueOf("foo"), beans.get(0).getValueType());
    }

    @Test
    public void testBuiltInColumnMappers() {
        h.createStatement("insert into testBean (valueType) values ('foo')").execute();

        List<String> strings = dao.listStrings();
        assertEquals(1, strings.size());
        assertEquals("foo", strings.get(0));

        List<TestEnum> enums = dao.listEnums();
        assertEquals(1, enums.size());
        assertEquals(TestEnum.foo, enums.get(0));
    }

    @Test
    public void testCustomColumnMapper() {
        h.createStatement("insert into testBean (valueType) values ('foo')").execute();

        List<ValueType> valueTypes = dao.listValueTypes();
        assertEquals(1, valueTypes.size());
        assertEquals(ValueType.valueOf("foo"), valueTypes.get(0));
    }
}
