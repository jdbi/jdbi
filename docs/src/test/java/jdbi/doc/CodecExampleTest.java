package jdbi.doc;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import jdbi.doc.Counter.CounterCodec;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.codec.CodecFactory;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class CodecExampleTest {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2();

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
        int result = jdbi.withHandle(h -> h.createUpdate("INSERT INTO counters (id, \"value\") VALUES (:id, :value)")
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
        Counter restoredCounter = jdbi.withHandle(h -> h.createQuery("SELECT \"value\" from counters where id = :id")
            .bind("id", counterId)
            .mapTo(COUNTER_TYPE).first());

        // end::load[]

        assertNotSame(counter, restoredCounter);

        int nextRestoredValue = restoredCounter.nextValue();

        assertNotEquals(counter.nextValue(), nextRestoredValue);
        assertEquals(nextValue, nextRestoredValue);
    }
}
