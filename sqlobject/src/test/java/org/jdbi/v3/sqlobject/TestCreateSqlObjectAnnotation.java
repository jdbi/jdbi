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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;

import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.exceptions.UnableToCreateSqlObjectException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestCreateSqlObjectAnnotation
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        db.getJdbi().registerRowMapper(new SomethingMapper());
        handle = db.getSharedHandle();
        handle.registerRowMapper(new SomethingMapper());
    }


    @Test
    public void testSimpleCreate() throws Exception
    {
        Foo foo = handle.attach(Foo.class);
        foo.insert(1, "Stephane");
        Something s = foo.createBar().findById(1);
        assertThat(s).isEqualTo(new Something(1, "Stephane"));
    }

    @Test
    public void testInsertAndFind() throws Exception
    {
        Foo foo = handle.attach(Foo.class);
        Something s = foo.insertAndFind(1, "Stephane");
        assertThat(s).isEqualTo(new Something(1, "Stephane"));
    }

    @Test
    public void testTransactionPropagates() throws Exception
    {
        Foo foo = db.getJdbi().open().attach(Foo.class);

        assertThatExceptionOfType(Exception.class).isThrownBy(() -> foo.insertAndFail(1, "Jeff"));

        Something n = foo.createBar().findById(1);
        assertThat(n).isNull();
    }

    public interface Foo
    {
        @CreateSqlObject
        Bar createBar();

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        int insert(@Bind("id") int id, @Bind("name") String name);

        @Transaction
        default Something insertAndFind(int id, String name) {
            insert(id, name);
            return createBar().findById(id);
        }

        @Transaction
        default Something insertAndFail(int id, String name) {
            insert(id, name);
            return createBar().explode();
        }
    }

    public interface Bar
    {
        @SqlQuery("select id, name from something where id = :id")
        Something findById(@Bind("id") int id);

        default Something explode()
        {
            throw new RuntimeException();
        }

    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testMeaningfulExceptionWhenWrongReturnTypeOfSqlUpdate() throws Exception {
        expectedException.expect(UnableToCreateSqlObjectException.class);
        expectedException.expectMessage("BogusSqlUpdateDao.getNames method is annotated with @SqlUpdate " +
                "so should return void, boolean, or Number but is returning: java.util.List<java.lang.String>");

        db.getJdbi().open().attach(BogusSqlUpdateDao.class);
    }

    public interface BogusSqlUpdateDao {
        @SqlUpdate("select name from something")
        List<String> getNames();
    }

    @Test
    public void testMeaningfulExceptionWhenWrongReturnTypeOfSqlBatch() throws Exception {
        expectedException.expect(UnableToCreateSqlObjectException.class);
        expectedException.expectMessage("BogusSqlBatchDao.getNames method is annotated with @SqlBatch " +
                "so should return void or int[] but is returning: int");

        db.getJdbi().open().attach(BogusSqlBatchDao.class);
    }

    public interface BogusSqlBatchDao {
        @SqlBatch("insert into table (a) values (:a)")
        int getNames(@Bind("a") String a);
    }
}
