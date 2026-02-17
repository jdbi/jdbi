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

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import jdbi.doc.Counter.CounterCodec;
import org.jdbi.core.Jdbi;
import org.jdbi.core.codec.CodecFactory;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class CodecSqlObjectTest {

    // tag::dao[]

    // SQL object dao
    public interface CounterDao {

        @SqlUpdate("INSERT INTO counters (id, \"value\") VALUES (:id, :value)")
        int storeCounter(@Bind("id") String id, @Bind("value") Counter counter);

        @SqlQuery("SELECT \"value\" from counters where id = :id")
        Counter loadCounter(@Bind("id") String id);
    }

    // end::dao[]

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2()
        .withPlugin(new SqlObjectPlugin());

    public static final QualifiedType<Counter> COUNTER_TYPE = QualifiedType.of(Counter.class);

    @BeforeEach
    public void setUp() {
        h2Extension.getJdbi().useHandle(h ->
            h.execute("CREATE TABLE counters (id VARCHAR PRIMARY KEY, \"value\" INT)"));
    }

    @Test
    public void testCounter() {
        Jdbi jdbi = h2Extension.getJdbi();

        // tag::register[]

        // register the codec with JDBI
        jdbi.registerCodecFactory(CodecFactory.forSingleCodec(COUNTER_TYPE, new CounterCodec()));

        // end::register[]

        Counter counter = new Counter();
        for (int i = 0; i < ThreadLocalRandom.current().nextInt(1000); i++) {
            counter.nextValue();
        }

        String counterId = UUID.randomUUID().toString();
        // tag::store[]

        // store object
        int result = jdbi.withExtension(CounterDao.class, dao -> dao.storeCounter(counterId, counter));

        // end::store[]

        assertThat(result).isOne();

        int nextValue = counter.nextValue();

        // increment counter
        counter.nextValue();
        counter.nextValue();
        counter.nextValue();

        // tag::load[]

        // load object
        Counter restoredCounter = jdbi.withExtension(CounterDao.class, dao -> dao.loadCounter(counterId));

        // end::load[]

        assertThat(counter).isNotSameAs(restoredCounter);

        int nextRestoredValue = restoredCounter.nextValue();

        assertThat(counter.nextValue()).isNotEqualTo(nextRestoredValue);
        assertThat(nextValue).isEqualTo(nextRestoredValue);
    }
}
