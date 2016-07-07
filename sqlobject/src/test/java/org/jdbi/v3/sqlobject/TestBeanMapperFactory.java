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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.ValueType;
import org.jdbi.v3.core.ValueTypeMapper;
import org.jdbi.v3.sqlobject.customizers.RegisterColumnMapper;
import org.jdbi.v3.sqlobject.customizers.RegisterBeanMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestBeanMapperFactory
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

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
        bar
    }

    @RegisterBeanMapper(TestBean.class)
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
        h = db.openHandle();
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
