package jdbi.doc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class StatementsTest
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    private Handle handle;

    @Before
    public void setUp()
    {
        handle = db.getSharedHandle();
        handle.execute("CREATE TABLE user (id INTEGER PRIMARY KEY, name VARCHAR)");
        handle.execute("INSERT INTO user VALUES (1, 'Alice')");
        handle.execute("INSERT INTO user VALUES (2, 'Bob')");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testQuery() throws Exception
    {
        // tag::query[]
        List<Map<String, Object>> users =
            handle.createQuery("SELECT id, name FROM user ORDER BY id ASC")
                .mapToMap()
                .list();

        assertThat(users).containsExactly(
                map("id", 1, "name", "Alice"),
                map("id", 2, "name", "Bob"));
        // end::query[]
    }

    @Test
    public void testUpdate() throws Exception
    {
        // tag::update[]
        int count = handle.createUpdate("INSERT INTO user(id, name) VALUES(:id, :name)")
            .bind("id", 3)
            .bind("name", "Charlie")
            .execute();
        assertThat(count).isEqualTo(1);
        // end::update[]
    }

    @Test
    public void testScript() throws Exception
    {
        // tag::script[]
        int[] results = handle.createScript(
                "INSERT INTO user VALUES(3, 'Charlie');" +
                "UPDATE user SET name='Bobby Tables' WHERE id=2;")
            .execute();

        assertThat(results).containsExactly(1, 1);
        // end::script[]
    }

    @Test
    public void testBatch() throws Exception
    {
        // tag::batch[]
        PreparedBatch batch = handle.prepareBatch("INSERT INTO user(id, name) VALUES(:id, :name)");
        for (int i = 100; i < 5000; i++)
        {
            batch.bind("id", i).bind("name", "User:" + i).add();
        }

        int[] expected = new int[4900];
        Arrays.fill(expected, 1);
        assertThat(batch.execute()).isEqualTo(expected);
        // end::batch[]
    }

    static <K, V> Map<K, V> map(K k1, V v1, K k2, V v2)
    {
        HashMap<K, V> h = new HashMap<>();
        h.put(k1, v1);
        h.put(k2, v2);
        return h;
    }
}
