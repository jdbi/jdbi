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
import org.jdbi.v3.sqlobject.unstable.BindIn;
import org.junit.After;
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

    @After
    public void tearDown() throws Exception
    {
        handle.execute("drop table test");
        handle.execute("drop table testNullable");
        handle.close();
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

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArray()
    {
        TestDao testDao = handle.attach(TestDao.class);
        String[] columnsArray = {"id", "name"};
        List<Object> values = new ArrayList<>();
        values.add(1);
        values.add("Some Pig");
        testDao.badInsert("test", columnsArray, values);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportedInsert()
    {
        TestDao testDao = handle.attach(TestDao.class);
        List<Object> values = new ArrayList<>();
        values.add(1);
        values.add("Some Pig");
        testDao.unsupportedInsert("test", testColumns, values);
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
        void insert(@Define("table") String table, @DefineIn("columns") List<String> columns, @BindIn("values") List<Object> values);

        @SqlUpdate("insert into <table> (<columns>) values (:id, :name)")
        void insert(@Define("table") String table, @DefineIn("columns") List<String> columns, @BindBean Something s);

        @SqlUpdate("insert into <table> (<columns>) values (<values>)")
        void badInsert(@Define("table") String table, @DefineIn("columns") Object columns, @BindIn("values") List<Object> values);

        @SqlUpdate("insert into <table> (<columns>) values (<values>)")
        void unsupportedInsert(@Define("table") String table, @DefineIn List<String> columns, @BindIn("values") List<Object> values);

        @SqlQuery("select <columns> from <table> where id = :id")
        Something findById(@DefineIn("columns") List<String> columns, @Define("table") String table, @Bind("id") long id);
    }
}
