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

import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.customizers.Define;
import org.jdbi.v3.sqlobject.customizers.DefineIn;
import org.jdbi.v3.sqlobject.customizers.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizers.BindIn;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestDefineInParameter
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;
    private List<String> testColumns;

    @Before
    public void setUp() throws Exception
    {
        handle = db.getSharedHandle();
        handle.execute("create table test (id identity primary key, name varchar(50))");
        handle.execute("create table testNullable (id identity primary key, name varchar(50) null)");
        testColumns = new ArrayList<>();
        testColumns.add("id");
        testColumns.add("name");
    }

    @Test
    public void testWithBindIn() throws Exception
    {
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
    public void testWithBindBean() throws Exception
    {
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
    public void testArray()
    {
        TestDao testDao = handle.attach(TestDao.class);
        String[] columnsArray = {"id", "name"};
        List<Object> values = new ArrayList<>();
        values.add(1);
        values.add("Some Pig");
        testDao.insert("test", columnsArray, values);
    }

    @Test
    public void testDefaultedNameInsert()
    {
        TestDao testDao = handle.attach(TestDao.class);
        List<Object> values = new ArrayList<>();
        values.add(1);
        values.add("Some Pig");
        testDao.defaultedInsert("test", testColumns, values);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyList()
    {
        TestDao testDao = handle.attach(TestDao.class);
        List<String> noColumns = new ArrayList<>();
        Something something = new Something(1, "Some Pig");
        testDao.insert("test", noColumns, something);
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface TestDao
    {
        @SqlUpdate("insert into <table> (<columns>) values (<values>)")
        void insert(@Define("table") String table, @DefineIn("columns") List<String> columns, @BindIn List<Object> values);

        @SqlUpdate("insert into <table> (<columns>) values (:id, :name)")
        void insert(@Define("table") String table, @DefineIn("columns") List<String> columns, @BindBean Something s);

        @SqlUpdate("insert into <table> (<columns>) values (<values>)")
        void insert(@Define("table") String table, @DefineIn("columns") Object[] columns, @BindIn List<Object> values);

        @SqlUpdate("insert into <table> (<columns>) values (<values>)")
        void defaultedInsert(@Define("table") String table, @DefineIn List<String> columns, @BindIn List<Object> values);

        @SqlQuery("select <columns> from <table> where id = :id")
        Something findById(@DefineIn("columns") List<String> columns, @Define("table") String table, @Bind("id") long id);
    }
}
