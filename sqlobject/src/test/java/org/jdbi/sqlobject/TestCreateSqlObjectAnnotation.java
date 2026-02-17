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
import org.jdbi.core.Something;
import org.jdbi.core.mapper.SomethingMapper;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.statement.SqlBatch;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.sqlobject.transaction.Transaction;
import org.jdbi.testing.junit5.JdbiExtension;
import org.jdbi.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestCreateSqlObjectAnnotation {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @BeforeEach
    public void setUp() {
        h2Extension.getJdbi().registerRowMapper(new SomethingMapper());
        handle = h2Extension.getSharedHandle();
        handle.registerRowMapper(new SomethingMapper());
    }

    @Test
    public void testSimpleCreate() {
        Foo foo = handle.attach(Foo.class);
        foo.insert(1, "Stephane");
        Something s = foo.createBar().findById(1);
        assertThat(s).isEqualTo(new Something(1, "Stephane"));
    }

    @Test
    public void testInsertAndFind() {
        Foo foo = handle.attach(Foo.class);
        Something s = foo.insertAndFind(1, "Stephane");
        assertThat(s).isEqualTo(new Something(1, "Stephane"));
    }

    @Test
    public void testTransactionPropagates() {
        Foo foo = h2Extension.getSharedHandle().attach(Foo.class);

        assertThatExceptionOfType(Exception.class).isThrownBy(() -> foo.insertAndFail(1, "Jeff"));

        Something n = foo.createBar().findById(1);
        assertThat(n).isNull();
    }

    @Test
    public void subObjectIsSqlObject() throws Exception {
        assertThat(h2Extension.getJdbi().withExtension(Foo.class, Foo::createBar)).isInstanceOf(SqlObject.class);
    }

    public interface Foo {
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

    public interface Bar {
        @SqlQuery("select id, name from something where id = :id")
        Something findById(@Bind("id") int id);

        default Something explode() {
            throw new RuntimeException();
        }

    }

    @Test
    public void testMeaningfulExceptionWhenWrongReturnTypeOfSqlUpdate() {

        assertThatThrownBy(() -> h2Extension.getSharedHandle().attach(BogusSqlUpdateDao.class))
                .isInstanceOf(UnableToCreateSqlObjectException.class)
                .hasMessage("BogusSqlUpdateDao.getNames method is annotated with @SqlUpdate "
                          + "and should return void, boolean, int, long, or have a @GetGeneratedKeys annotation, but is returning: java.util.List<java.lang.String>");
    }

    public interface BogusSqlUpdateDao {
        @SqlUpdate("select name from something")
        List<String> getNames();
    }

    @Test
    public void testMeaningfulExceptionWhenWrongReturnTypeOfSqlBatch() {
        assertThatThrownBy(() -> h2Extension.getSharedHandle().attach(BogusSqlBatchDao.class))
                .isInstanceOf(UnableToCreateSqlObjectException.class)
                .hasMessageContaining("BogusSqlBatchDao.getNames method is annotated with @SqlBatch "
                                    + "so should return void, int[], or boolean[] but is returning: int");
    }

    public interface BogusSqlBatchDao {
        @SqlBatch("insert into table (a) values (:a)")
        int getNames(@Bind("a") String a);
    }
}
