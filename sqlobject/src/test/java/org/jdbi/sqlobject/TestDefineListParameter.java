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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.jdbi.core.Handle;
import org.jdbi.core.Something;
import org.jdbi.core.mapper.SomethingMapper;
import org.jdbi.sqlobject.config.RegisterRowMapper;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.customizer.BindBean;
import org.jdbi.sqlobject.customizer.BindList;
import org.jdbi.sqlobject.customizer.Define;
import org.jdbi.sqlobject.customizer.DefineList;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestDefineListParameter {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin());

    private final String[] columnsArray = new String[]{"id", "name"};
    private final List<String> testColumns = Arrays.asList(columnsArray);

    private Handle handle;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
        handle.execute("create table test (id identity primary key, name varchar(50))");
        handle.execute("create table testNullable (id identity primary key, name varchar(50) null)");
    }

    @Test
    public void testWithBindList() {
        TestDao testDao = handle.attach(TestDao.class);

        List<Object> values = new ArrayList<>();
        values.add(1);
        values.add("Some Pig");

        List<Object> valuesNull = new ArrayList<>();
        valuesNull.add(2);
        valuesNull.add(null);

        testDao.insert("test", testColumns, values);
        testDao.insert("testNullable", testColumns, valuesNull);

        Something something = new Something(1, "Some Pig");
        Something nothing = new Something(2, null);

        assertThat(testDao.findById(testColumns, "test", 1)).isEqualTo(something);
        assertThat(testDao.findById(testColumns, "testNullable", 1)).isNull();

        assertThat(testDao.findById(testColumns, "test", 2)).isNull();
        assertThat(testDao.findById(testColumns, "testNullable", 2)).isEqualTo(nothing);
    }

    @Test
    public void testWithBindBean() {
        TestDao testDao = handle.attach(TestDao.class);

        Something something = new Something(1, "Some Pig");
        Something nothing = new Something(2, null);

        testDao.insert("test", testColumns, something);
        testDao.insert("testNullable", testColumns, nothing);

        assertThat(testDao.findById(testColumns, "test", 1)).isEqualTo(something);
        assertThat(testDao.findById(testColumns, "testNullable", 1)).isNull();

        assertThat(testDao.findById(testColumns, "test", 2)).isNull();
        assertThat(testDao.findById(testColumns, "testNullable", 2)).isEqualTo(nothing);
    }

    @Test
    public void testArray() {
        TestDao testDao = handle.attach(TestDao.class);
        testDao.insert("test", columnsArray, ImmutableList.of(1, "Some Pig"));

        assertThat(testDao.findById(testColumns, "test", 1)).isEqualTo(new Something(1, "Some Pig"));
    }

    @Test
    public void nullHostileContains() {
        TestDao testDao = handle.attach(TestDao.class);
        List<String> columns = new AbstractList<String>() {
            @Override
            public int size() {
                return testColumns.size();
            }
            @Override
            public String get(int index) {
                return testColumns.get(index);
            }
            @Override
            public boolean contains(Object o) {
                if (o == null) {
                    throw new NullPointerException();
                }
                return testColumns.contains(o);
            }
        };
        List<Object> values = new ArrayList<>();
        values.add(1);
        values.add("Some Pig");
        testDao.insert("test", columns, values);

        assertThat(testDao.findById(testColumns, "test", 1)).isEqualTo(new Something(1, "Some Pig"));
    }

    @Test
    public void testDefaultedNameInsert() {
        TestDao testDao = handle.attach(TestDao.class);
        List<Object> values = new ArrayList<>();
        values.add(1);
        values.add("Some Pig");
        testDao.defaultedInsert("test", testColumns, values);

        assertThat(testDao.findById(testColumns, "test", 1)).isEqualTo(new Something(1, "Some Pig"));
    }

    @Test
    public void testEmptyList() {
        TestDao testDao = handle.attach(TestDao.class);
        List<String> noColumns = new ArrayList<>();
        Something something = new Something(1, "Some Pig");
        assertThatThrownBy(() -> testDao.insert("test", noColumns, something)).isInstanceOf(IllegalArgumentException.class);
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface TestDao {
        @SqlUpdate("insert into <table> (<columns>) values (<values>)")
        void insert(@Define("table") String table, @DefineList("columns") List<String> columns, @BindList List<Object> values);

        @SqlUpdate("insert into <table> (<columns>) values (:id, :name)")
        void insert(@Define("table") String table, @DefineList("columns") List<String> columns, @BindBean Something s);

        @SqlUpdate("insert into <table> (<columns>) values (<values>)")
        void insert(@Define("table") String table, @DefineList("columns") Object[] columns, @BindList List<Object> values);

        @SqlUpdate("insert into <table> (<columns>) values (<values>)")
        void defaultedInsert(@Define("table") String table, @DefineList List<String> columns, @BindList List<Object> values);

        @SqlQuery("select <columns> from <table> where id = :id")
        Something findById(@DefineList("columns") List<String> columns, @Define("table") String table, @Bind("id") long id);
    }
}
