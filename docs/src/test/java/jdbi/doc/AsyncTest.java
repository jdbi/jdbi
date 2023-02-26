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

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.async.JdbiExecutor;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

public class AsyncTest {

    public interface SomethingDao {

        @SqlQuery("SELECT name FROM contacts WHERE id = ?")
        String getName(int id);
    }

    private Jdbi jdbi;
    private JdbiExecutor jdbiExecutor;
    private Handle handle;

    @BeforeEach
    public void setUp() {
        // H2 in-memory database
        Jdbi jdbi = Jdbi.create("jdbc:h2:mem:test").installPlugin(new SqlObjectPlugin());

        // tag::createJdbiExecutor[]
        Executor executor = Executors.newFixedThreadPool(8);
        JdbiExecutor jdbiExecutor = JdbiExecutor.create(jdbi, executor);
        // end::createJdbiExecutor[]

        this.jdbi = jdbi;
        this.jdbiExecutor = jdbiExecutor;
        this.handle = jdbi.open();

        jdbi.useHandle(handle -> {
            handle.execute("create table contacts (id int primary key, name varchar(100))");
            handle.execute("insert into contacts (id, name) values (?, ?)", 1, "Alice");
            handle.execute("insert into contacts (id, name) values (?, ?)", 2, "Bob");
        });
    }

    @AfterEach
    public void tearDown() {
        handle.close();
    }

    @Test
    public void withHandle() {
        // tag::withHandle[]
        CompletionStage<List<String>> futureResult = jdbiExecutor.withHandle(handle -> {
            return handle.createQuery("select name from contacts where id = 1")
                .mapTo(String.class)
                .list();
        });

        assertThat(futureResult)
            .succeedsWithin(Duration.ofSeconds(10))
            .asList()
            .contains("Alice");
        // end::withHandle[]
    }

    @Test
    public void useHandle() {
        // tag::useHandle[]
        CompletionStage<Void> futureResult = jdbiExecutor.useHandle(handle -> {
            handle.execute("insert into contacts (id, name) values (?, ?)", 3, "Erin");
        });

        // wait for stage to complete (don't do this in production code!)
        futureResult.toCompletableFuture().join();
        assertThat(handle.createQuery("select name from contacts where id = 3")
            .mapTo(String.class)
            .list()).contains("Erin");
        // end::useHandle[]
    }

    @Test
    public void withExtension() {
        // tag::withExtension[]
        CompletionStage<String> futureResult =
            jdbiExecutor.withExtension(SomethingDao.class, dao -> dao.getName(1));

        assertThat(futureResult)
            .succeedsWithin(Duration.ofSeconds(10))
            .isEqualTo("Alice");
        // end::withExtension[]
    }

    @Test
    public void failsWhenReturningAnIterator() {
        // tag::failReturningIterator[]
        CompletionStage<Iterator<String>> futureResult = jdbiExecutor.withHandle(handle -> {
            return handle.createQuery("select name from contacts where id = 1")
                .mapTo(String.class)
                .iterator();
        });

        // wait for stage to complete (don't do this in production code!)
        Iterator<String> result = futureResult.toCompletableFuture().join();
        // result.hasNext() fails because the handle is already closed at this point
        assertThatException().isThrownBy(() -> result.hasNext());
        // end::failReturningIterator[]
    }
}
