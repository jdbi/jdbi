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
package org.jdbi.v3.json;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.mapper.reflect.FieldMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindFields;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractIssue2395Test {

    Handle handle;

    protected abstract Handle getHandle();

    @BeforeEach
    void setUp() {
        this.handle = getHandle();
        // register a mapper for the wrapper class that contains a json field.
        this.handle.registerRowMapper(ConstructorWrapper.class, ConstructorMapper.of(ConstructorWrapper.class));
        this.handle.registerRowMapper(FieldWrapper.class, FieldMapper.of(FieldWrapper.class));
        this.handle.registerRowMapper(BeanWrapper.class, BeanMapper.of(BeanWrapper.class));

        this.handle.execute("create table test2395 (id integer not null primary key, name varchar(255), data json)");
    }

    @Test
    @DisplayName("Test storing and retrieving a nested json field.")
    void testConstructorWrapperIssue2395() {
        final ConstructorWrapper data1 = new ConstructorWrapper(1, "alice",
            Arrays.asList(new JsonData("first", "one", 4), new JsonData("second", "two", 8), new JsonData("third", "three", 15),
                new JsonData("fourth", "four", 16), new JsonData("fifth", "five", 23), new JsonData("sixth", "six", 42)));
        final ConstructorWrapper data2 = new ConstructorWrapper(2, "bob", Collections.singletonList(new JsonData("toast", "words", 8)));

        CtorDao dao = handle.attach(CtorDao.class);

        dao.write(data1);
        dao.write(data2);

        List<ConstructorWrapper> data = dao.readAll();
        assertThat(data).containsExactly(data1, data2);

        assertThat(dao.retrieve(1)).isEqualTo(data1);
        assertThat(dao.retrieve(2)).isEqualTo(data2);
    }

    @Test
    @DisplayName("Test storing and retrieving a nested json field.")
    void testFieldWrapperIssue2395() {
        final FieldWrapper data1 = new FieldWrapper();
        data1.id = 1;
        data1.name = "alice";
        data1.data = Arrays.asList(new JsonData("first", "one", 4), new JsonData("second", "two", 8), new JsonData("third", "three", 15),
            new JsonData("fourth", "four", 16), new JsonData("fifth", "five", 23), new JsonData("sixth", "six", 42));

        final FieldWrapper data2 = new FieldWrapper();
        data2.id = 2;
        data2.name = "bob";
        data2.data = Collections.singletonList(new JsonData("toast", "words", 8));

        FieldDao dao = handle.attach(FieldDao.class);

        dao.write(data1);
        dao.write(data2);

        List<FieldWrapper> data = dao.readAll();
        assertThat(data).containsExactly(data1, data2);

        assertThat(dao.retrieve(1)).isEqualTo(data1);
        assertThat(dao.retrieve(2)).isEqualTo(data2);
    }

    @Test
    @DisplayName("Test storing and retrieving a nested json field.")
    void testBeanWrapperIssue2395() {
        final BeanWrapper data1 = new BeanWrapper();
        data1.setId(1);
        data1.setName("alice");
        data1.setData(Arrays.asList(new JsonData("first", "one", 4), new JsonData("second", "two", 8), new JsonData("third", "three", 15),
            new JsonData("fourth", "four", 16), new JsonData("fifth", "five", 23), new JsonData("sixth", "six", 42)));

        final BeanWrapper data2 = new BeanWrapper();
        data2.setId(2);
        data2.setName("bob");
        data2.setData(Collections.singletonList(new JsonData("toast", "words", 8)));

        BeanDao dao = handle.attach(BeanDao.class);

        dao.write(data1);
        dao.write(data2);

        List<BeanWrapper> data = dao.readAll();
        assertThat(data).containsExactly(data1, data2);

        assertThat(dao.retrieve(1)).isEqualTo(data1);
        assertThat(dao.retrieve(2)).isEqualTo(data2);
    }

    public interface CtorDao {

        @SqlUpdate("insert into test2395 (id, name, data) values (:id, :name, :data)")
        void write(@BindBean ConstructorWrapper bean);

        @SqlQuery("select * from test2395")
        List<ConstructorWrapper> readAll();

        @SqlQuery("select * from test2395 where id = :id")
        ConstructorWrapper retrieve(int id);
    }


    public interface FieldDao {

        @SqlUpdate("insert into test2395 (id, name, data) values (:id, :name, :data)")
        void write(@BindFields FieldWrapper bean);

        @SqlQuery("select * from test2395")
        List<FieldWrapper> readAll();

        @SqlQuery("select * from test2395 where id = :id")
        FieldWrapper retrieve(int id);
    }

    public interface BeanDao {

        @SqlUpdate("insert into test2395 (id, name, data) values (:id, :name, :data)")
        void write(@BindBean BeanWrapper bean);

        @SqlQuery("select * from test2395")
        List<BeanWrapper> readAll();

        @SqlQuery("select * from test2395 where id = :id")
        BeanWrapper retrieve(int id);
    }

    public static class ConstructorWrapper {

        private final int id;
        private final String name;
        private final List<JsonData> data;

        public ConstructorWrapper(int id, String name, @Json List<JsonData> data) {
            this.id = id;
            this.name = name;
            this.data = data;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @Json
        public List<JsonData> getData() {
            return data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ConstructorWrapper wrapper = (ConstructorWrapper) o;
            return id == wrapper.id && Objects.equals(name, wrapper.name) && Objects.equals(data, wrapper.data);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, data);
        }
    }

    public static class FieldWrapper {

        public int id;
        public String name;

        @Json
        public List<JsonData> data;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            FieldWrapper wrapper = (FieldWrapper) o;
            return id == wrapper.id && Objects.equals(name, wrapper.name) && Objects.equals(data, wrapper.data);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, data);
        }
    }

    public static class BeanWrapper {

        private int id;
        private String name;
        private List<JsonData> data;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Json
        public List<JsonData> getData() {
            return data;
        }

        public void setData(List<JsonData> data) {
            this.data = data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BeanWrapper wrapper = (BeanWrapper) o;
            return id == wrapper.id && Objects.equals(name, wrapper.name) && Objects.equals(data, wrapper.data);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, data);
        }
    }

    public static class JsonData {

        private final String key1;
        private final String key2;
        private final int key3;

        public JsonData(String key1, String key2, int key3) {
            this.key1 = key1;
            this.key2 = key2;
            this.key3 = key3;
        }

        public String getKey1() {
            return key1;
        }

        public String getKey2() {
            return key2;
        }

        public int getKey3() {
            return key3;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            JsonData jsonData = (JsonData) o;
            return key3 == jsonData.key3 && Objects.equals(key1, jsonData.key1) && Objects.equals(key2, jsonData.key2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key1, key2, key3);
        }
    }
}
