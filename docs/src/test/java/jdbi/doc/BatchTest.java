package jdbi.doc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;

import org.jdbi.v3.core.Batch;
import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.PreparedBatch;
import org.jdbi.v3.sqlobject.BindBean;
import org.jdbi.v3.sqlobject.SqlBatch;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.SqlQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class BatchTest {

    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());
    private Handle handle;

    @Before
    public void getHandle() {
        handle = db.getSharedHandle();
        handle.execute("CREATE TABLE fruit (id INT PRIMARY KEY, name VARCHAR)");
    }

    @Test
    // tag::simpleBatch[]
    public void testSimpleBatch() {
        Batch batch = handle.createBatch();

        batch.add("INSERT INTO fruit VALUES(0, 'apple')");
        batch.add("INSERT INTO fruit VALUES(1, 'banana')");

        int[] rowsModified = batch.execute();
        assertThat(rowsModified).containsExactly(1, 1);
        assertThat(handle.createQuery("SELECT count(1) FROM fruit")
                .mapTo(int.class)
                .findOnly()
                .intValue())
                .isEqualTo(2);
    }
    // end::simpleBatch[]

    @Test
    // tag::preparedBatch[]
    public void testPreparedBatch() {
        PreparedBatch batch = handle.prepareBatch("INSERT INTO fruit VALUES(:id, :name)");

        batch.bind("id", 0);
        batch.bind("name", "apple");
        batch.add();

        batch.bind("id", 1);
        batch.bind("name", "banana");
        batch.add();

        int[] rowsModified = batch.execute();
        assertThat(rowsModified).containsExactly(1, 1);
        assertThat(handle.createQuery("SELECT count(1) FROM fruit")
                .mapTo(int.class)
                .findOnly()
                .intValue())
                .isEqualTo(2);
    }
    // end::preparedBatch[]

    @Test
    // tag::sqlObjectBatch[]
    public void testSqlObjectBatch() {
        BasketOfFruit basket = handle.attach(BasketOfFruit.class);

        int[] rowsModified = basket.fillBasket(Arrays.asList(
                new Fruit(0, "apple"),
                new Fruit(1, "banana")));

        assertThat(rowsModified).containsExactly(1, 1);
        assertThat(basket.countFruit()).isEqualTo(2);
    }

    public interface BasketOfFruit {
        @SqlBatch("INSERT INTO fruit VALUES(:id, :name)")
        int[] fillBasket(@BindBean Collection<Fruit> fruits);

        @SqlQuery("SELECT count(1) FROM fruit")
        int countFruit();
    }

    public class Fruit {
        private final int id;
        private final String name;

        Fruit(int id, String name) {
            this.id = id; this.name = name;
        }

        public int getId() { return id; }
        public String getName() { return name; }
    }
    // end::sqlObjectBatch[]
}
