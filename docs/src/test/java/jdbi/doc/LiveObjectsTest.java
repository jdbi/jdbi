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
package jdbi.doc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.result.ResultSetException;
import org.jdbi.sqlobject.CreateSqlObject;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LiveObjectsTest {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2()
            .withInitializer(TestingInitializers.usersWithData())
            .withPlugin(new SqlObjectPlugin());

    // tag::dao[]
    interface UserDao {
        @SqlQuery("SELECT name FROM users")
        Stream<String> getNamesAsStream();

        default List<String> getNames() {  // <1>
            try (Stream<String> stream = getNamesAsStream()) { // <2>
                return stream.collect(Collectors.toList()); // <3>
            }
        }
    }
    // end::dao[]

    // tag::callback-dao[]
    interface CallbackDao {
        @SqlQuery("SELECT name FROM users")
         void getNamesAsStream(Consumer<Stream<String>> consumer);

        @SqlQuery("SELECT name FROM users")
        Set<String> getNamesAsSet(Function<Stream<String>, Set<String>> function);

    }
    // end::callback-dao[]

    // tag::nested-dao[]
    interface NestedDao {
        @CreateSqlObject
        UserDao userDao(); // <1>

        default List<String> getNames() {
            try (Stream<String> stream = userDao().getNamesAsStream()) {
                return stream.collect(Collectors.toList());
            }
        }
    }
    // end::nested-dao[]

    interface TestDao {
        @SqlQuery("SELECT name FROM users")
        List<String> getNames();
    }

    private Jdbi jdbi;
    private List<String> names;

    @BeforeEach
    void setUp() {
        this.jdbi = h2Extension.getJdbi();
        this.names = this.jdbi.withExtension(TestDao.class, dao -> dao.getNames());
    }

    // tag::attach[]
    @Test
    void testHandleAttach() {
        try (Handle handle = jdbi.open()) { // <1>
            UserDao dao = handle.attach(UserDao.class);
            try (Stream<String> stream = dao.getNamesAsStream()) { // <2>
                List<String> result = stream.collect(Collectors.toList()); // <3>
                assertThat(result).containsAll(names);
            }
        }
    }
    // end::attach[]

    // tag::on-demand[]
    @Test
    void testOnDemandFails() {
        assertThatThrownBy(() -> {
            UserDao dao = jdbi.onDemand(UserDao.class);
            List<String> result = dao.getNamesAsStream().collect(Collectors.toList()); // <1>
            assertThat(result).containsAll(names);
        }).isInstanceOf(ResultSetException.class); // <2>
    }
    // end::on-demand[]

    // tag::extension[]
    @Test
    void testWithExtensionFails() {
        assertThatThrownBy(() -> {
            List<String> result = jdbi.withExtension(UserDao.class, UserDao::getNamesAsStream).collect(Collectors.toList()); // <1>
            assertThat(result).containsAll(names);
        }).isInstanceOf(ResultSetException.class); // <2>
    }
    // end::extension[]

    // tag::on-demand-default[]
    @Test
    void testOnDemandDefaultMethod() {
        UserDao dao = jdbi.onDemand(UserDao.class);
        List<String> result = dao.getNames();
        assertThat(result).containsAll(names);
    }
    // end::on-demand-default[]

    // tag::extension-default[]
    @Test
    void testWithExtensionDefaultMethod() {
        List<String> result = jdbi.withExtension(UserDao.class, UserDao::getNames);
        assertThat(result).containsAll(names);
    }
    // end::extension-default[]

    // tag::attach-callback[]
    @Test
    void testHandleAttachConsumer() {
        try (Handle handle = jdbi.open()) { // <1>
            CallbackDao dao = handle.attach(CallbackDao.class);
            List<String> result = new ArrayList<>();
            dao.getNamesAsStream(stream -> stream.forEach(result::add)); // <2>
            assertThat(result).containsAll(names);
        }
    }

    @Test
    void testHandleAttachFunction() {
        try (Handle handle = jdbi.open()) { // <1>
            CallbackDao dao = handle.attach(CallbackDao.class);
            Set<String> result = dao.getNamesAsSet(stream -> stream.collect(Collectors.toSet())); // <2>
            assertThat(result).containsAll(names);
        }
    }
    // end::attach-callback[]

    // tag::on-demand-callback[]
    @Test
    void testOnDemandConsumer() {
        CallbackDao dao = jdbi.onDemand(CallbackDao.class);
        List<String> result = new ArrayList<>();
        dao.getNamesAsStream(stream -> stream.forEach(result::add));
        assertThat(result).containsAll(names);
    }

    @Test
    void testOnDemandFunction() {
        CallbackDao dao = jdbi.onDemand(CallbackDao.class);
        Set<String> result = dao.getNamesAsSet(stream -> stream.collect(Collectors.toSet()));
        assertThat(result).containsAll(names);
    }
    // end::on-demand-callback[]

    // tag::extension-callback[]
    @Test
    void testWithExtensionConsumer() {
        List<String> result = new ArrayList<>();
        jdbi.useExtension(CallbackDao.class,
            dao -> dao.getNamesAsStream(stream -> stream.forEach(result::add))); // <1>
        assertThat(result).containsAll(names);
    }

    @Test
    void testWithExtensionFunction() {
        Set<String> result = jdbi.withExtension(CallbackDao.class,
            dao -> dao.getNamesAsSet(stream -> stream.collect(Collectors.toSet())));

        assertThat(result).containsAll(names);
    }
    // end::extension-callback[]

    // tag::on-demand-nested[]
    @Test
    void testOnDemandNestedMethod() {
        NestedDao dao = jdbi.onDemand(NestedDao.class);
        List<String> result = dao.getNames();
        assertThat(result).containsAll(names);
    }
    // end::on-demand-nested[]
}
