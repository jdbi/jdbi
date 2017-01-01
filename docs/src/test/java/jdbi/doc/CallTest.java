package jdbi.doc;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Types;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.postgres.PostgresDbRule;
import org.junit.Rule;
import org.junit.Test;

public class CallTest
{
    @Rule
    public PostgresDbRule db = new PostgresDbRule();

    @Test
    public void testCall()
    {
        Handle handle = db.getSharedHandle();
        // tag::call[]
        handle.execute(
                "CREATE FUNCTION the_answer(answer INOUT INTEGER) AS $$" +
                    "BEGIN answer := 42; END;" +
                "$$ LANGUAGE plpgsql");

        assertThat(handle.createCall("{? = call the_answer(?)}")
                .registerOutParameter(0, Types.INTEGER)
                .bind(1, 13)
                .invoke()
                .getInt(0))
            .isEqualTo(42);
        // end::call[]
    }
}
