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

import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.ValueType;
import org.jdbi.v3.core.ValueTypeMapper;
import org.jdbi.v3.sqlobject.customizers.RegisterColumnMapper;
import org.jdbi.v3.sqlobject.customizers.RegisterFieldMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestFieldMapper
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    public static class TestObject
    {
        ValueType valueType;

        public ValueType getValueType() {
            return valueType;
        }
    }

    @RegisterColumnMapper(ValueTypeMapper.class)
    public interface TestDao
    {
        @SqlQuery("select * from testBean")
        @RegisterFieldMapper(TestObject.class)
        List<TestObject> listBeans();

        @SqlQuery("select valueType as obj_value_type from testBean")
        @RegisterFieldMapper(value=TestObject.class, prefix="obj_")
        List<TestObject> listBeansPrefix();
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
    public void testMapFields() {
        h.createUpdate("insert into testBean (valueType) values ('foo')").execute();

        List<TestObject> beans = dao.listBeans();
        assertThat(beans).extracting(TestObject::getValueType).containsExactly(ValueType.valueOf("foo"));
    }

    @Test
    public void testMapFieldsPrefix() {
        h.createUpdate("insert into testBean (valueType) values ('foo')").execute();

        List<TestObject> beans = dao.listBeansPrefix();
        assertThat(beans).extracting(TestObject::getValueType).containsExactly(ValueType.valueOf("foo"));
    }
}
