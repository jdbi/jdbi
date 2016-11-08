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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.ValueType;
import org.jdbi.v3.core.ValueTypeMapper;
import org.jdbi.v3.sqlobject.customizers.RegisterColumnMapper;
import org.jdbi.v3.sqlobject.customizers.RegisterBeanMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestBeanMapper
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

    @RegisterColumnMapper(ValueTypeMapper.class)
    public interface TestDao
    {
        @SqlQuery("select * from testBean")
        @RegisterBeanMapper(TestBean.class)
        List<TestBean> listBeans();

        @SqlQuery("select valueType as bean_value_type from testBean")
        @RegisterBeanMapper(value=TestBean.class, prefix="bean_")
        List<TestBean> listBeansPrefix();
    }

    Handle h;
    TestDao dao;

    @Before
    public void createTable() throws Exception {
        h = db.openHandle();
        h.createUpdate("create table testBean (valueType varchar(50))").execute();
        dao = h.attach(TestDao.class);
    }

    @Test
    public void testMapBean() {
        h.createUpdate("insert into testBean (valueType) values ('foo')").execute();

        List<TestBean> beans = dao.listBeans();
        assertThat(beans).extracting(TestBean::getValueType).containsExactly(ValueType.valueOf("foo"));
    }

    @Test
    public void testMapBeanPrefix() {
        h.createUpdate("insert into testBean (valueType) values ('foo')").execute();

        List<TestBean> beans = dao.listBeansPrefix();
        assertThat(beans).extracting(TestBean::getValueType).containsExactly(ValueType.valueOf("foo"));
    }
}
