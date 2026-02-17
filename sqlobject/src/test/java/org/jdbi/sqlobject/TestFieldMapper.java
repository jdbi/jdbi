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

package org.jdbi.sqlobject;

import java.util.List;

import org.jdbi.core.Handle;
import org.jdbi.core.ValueType;
import org.jdbi.core.mapper.ValueTypeMapper;
import org.jdbi.sqlobject.config.RegisterColumnMapper;
import org.jdbi.sqlobject.config.RegisterFieldMapper;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestFieldMapper {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin());

    public static class TestObject {

        ValueType valueType;

        public ValueType getValueType() {
            return valueType;
        }
    }

    @RegisterColumnMapper(ValueTypeMapper.class)
    public interface TestDao {
        @SqlQuery("select * from testBean")
        @RegisterFieldMapper(TestObject.class)
        List<TestObject> listBeans();

        @SqlQuery("select valueType as obj_value_type from testBean")
        @RegisterFieldMapper(value = TestObject.class, prefix = "obj_")
        List<TestObject> listBeansPrefix();
    }

    Handle h;
    TestDao dao;

    @BeforeEach
    public void createTable() {
        h = h2Extension.openHandle();
        h.createUpdate("create table testBean (valueType varchar(50))").execute();
        dao = h.attach(TestDao.class);
    }

    @AfterEach
    public void tearDown() {
        h.close();
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
