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
package org.jdbi.v3.sqlobject.statement;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.FetchSize;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestSqlPreflight {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
    }

    @Test
    public void preflightRunsBeforeMainStatementOnSameHandle() {
        Dao dao = handle.attach(Dao.class);

        // the preflight inserts a row that the main query then sees
        assertThat(dao.staticPreflightThenSelect()).containsExactly("preflight");
    }

    @Test
    public void preflightBindsMethodArguments() {
        Dao dao = handle.attach(Dao.class);

        assertThat(dao.insertThenFind(5, "bound")).containsExactly("bound");
    }

    @Test
    public void multiplePreflightsRunInDeclarationOrder() {
        Dao dao = handle.attach(Dao.class);

        assertThat(dao.twoPreflights()).containsExactly("a", "b");
    }

    @Test
    public void staticPreflightWithMethodArgumentsDoesNotFailUnusedBindingCheck() {
        Dao dao = handle.attach(Dao.class);

        // the issue's example: a static preflight on a method that takes a (here unrelated) argument.
        // The argument is bound to the preflight statement too, and must not trip the unused-binding check.
        assertThat(dao.searchWithStaticPreflight("preflight")).containsExactly("preflight");
    }

    @Test
    public void preflightCoexistsWithQueryOnlyCustomizer() {
        Dao dao = handle.attach(Dao.class);

        // @FetchSize only applies to queries; the preflight path must not apply it to its Update
        assertThat(dao.withFetchSize()).contains("fetchSize");
    }

    @Test
    public void preflightWorksWithSqlUpdateAsMainStatement() {
        Dao dao = handle.attach(Dao.class);

        dao.preflightThenUpdate();

        assertThat(handle.createQuery("select name from something order by name").mapTo(String.class).list())
                .containsExactly("main", "pre");
    }

    @Test
    public void preflightRunsInsideTransactionRegardlessOfAnnotationOrder() {
        // The preflight runs as a customizer on the main statement, so it is always inside the
        // method's @Transaction, without any @ExtensionHandlerCustomizationOrder hint and regardless
        // of which of @Transaction / @SqlPreflight is declared first. The main statement throws,
        // rolling back the transaction, so the preflight's insert must not survive either way.
        TxnDao dao = handle.attach(TxnDao.class);

        assertThatThrownBy(dao::txnBeforePreflight).isInstanceOf(Exception.class);
        assertThat(handle.createQuery("select count(*) from something").mapTo(Integer.class).one())
                .describedAs("@Transaction declared before @SqlPreflight: preflight insert rolled back")
                .isZero();

        assertThatThrownBy(dao::preflightBeforeTxn).isInstanceOf(Exception.class);
        assertThat(handle.createQuery("select count(*) from something").mapTo(Integer.class).one())
                .describedAs("@SqlPreflight declared before @Transaction: preflight insert rolled back")
                .isZero();
    }

    @Test
    public void typeLevelPreflightApplies() {
        TypeLevelDao dao = handle.attach(TypeLevelDao.class);

        assertThat(dao.all()).containsExactly("type");
    }

    @Test
    public void typeAndMethodPreflightsEachRunExactlyOnce() {
        // both inserts use a fixed primary key; running either preflight twice would raise a
        // primary-key violation, so a successful count of 2 proves single execution per level.
        BothLevelsDao dao = handle.attach(BothLevelsDao.class);

        assertThat(dao.count()).isEqualTo(2);
    }

    public interface Dao {

        @SqlPreflight("insert into something (id, name) values (1, 'preflight')")
        @SqlQuery("select name from something order by id")
        List<String> staticPreflightThenSelect();

        @SqlPreflight("insert into something (id, name) values (:id, :name)")
        @SqlQuery("select name from something where id = :id")
        List<String> insertThenFind(@Bind("id") int id, @Bind("name") String name);

        @SqlPreflight("insert into something (id, name) values (1, 'a')")
        @SqlPreflight("insert into something (id, name) values (2, 'b')")
        @SqlQuery("select name from something order by id")
        List<String> twoPreflights();

        @SqlPreflight("insert into something (id, name) values (1, 'preflight')")
        @SqlQuery("select name from something where name = :search")
        List<String> searchWithStaticPreflight(@Bind("search") String search);

        @SqlPreflight("insert into something (id, name) values (1, 'fetchSize')")
        @FetchSize(1)
        @SqlQuery("select name from something order by id")
        List<String> withFetchSize();

        @SqlPreflight("insert into something (id, name) values (1, 'pre')")
        @SqlUpdate("insert into something (id, name) values (2, 'main')")
        void preflightThenUpdate();
    }

    public interface TxnDao {

        @Transaction
        @SqlPreflight("insert into something (id, name) values (1, 'preflight')")
        @SqlUpdate("this is not valid sql and will throw")
        void txnBeforePreflight();

        @SqlPreflight("insert into something (id, name) values (1, 'preflight')")
        @Transaction
        @SqlUpdate("this is not valid sql and will throw")
        void preflightBeforeTxn();
    }

    @SqlPreflight("insert into something (id, name) values (1, 'type')")
    public interface TypeLevelDao {

        @SqlQuery("select name from something order by id")
        List<String> all();
    }

    @SqlPreflight("insert into something (id, name) values (1, 'type')")
    public interface BothLevelsDao {

        @SqlPreflight("insert into something (id, name) values (2, 'method')")
        @SqlQuery("select count(*) from something")
        int count();
    }
}
