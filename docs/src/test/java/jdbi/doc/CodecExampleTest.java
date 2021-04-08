package jdbi.doc;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import jdbi.doc.Counter.CounterCodec;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.codec.CodecFactory;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.rule.DatabaseRule;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;

public class CodecExampleTest {

    @Rule
    public DatabaseRule dbRule = new H2DatabaseRule();

    public static final QualifiedType<Counter> COUNTER_TYPE = QualifiedType.of(Counter.class);

    @Before
    public void setUp() {
        dbRule.getJdbi().useHandle(h -> {
            h.execute("CREATE TABLE counters (id VARCHAR PRIMARY KEY, value INT)");
        });
    }

    @Test
    public void testCounter() {
        Jdbi jdbi = dbRule.getJdbi();

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
        int result = jdbi.withHandle(h -> h.createUpdate("INSERT INTO counters (id, value) VALUES (:id, :value)")
            .bind("id", counterId)
            .bindByType("value", counter, COUNTER_TYPE)
            .execute());

        // end::store[]

        int nextValue = counter.nextValue();

        // increment counter
        counter.nextValue();
        counter.nextValue();
        counter.nextValue();

        // tag::load[]

        // load object
        Counter restoredCounter = jdbi.withHandle(h -> h.createQuery("SELECT value from counters where id = :id")
            .bind("id", counterId)
            .mapTo(COUNTER_TYPE).first());

        // end::load[]

        assertNotSame(counter, restoredCounter);

        int nextRestoredValue = restoredCounter.nextValue();

        assertNotEquals(counter.nextValue(), nextRestoredValue);
        assertEquals(nextValue, nextRestoredValue);
    }
}
